package com.ocnilentes.mediagenda.modules.reminder.application;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentService;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.AppointmentReminder;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import com.ocnilentes.mediagenda.modules.reminder.domain.port.AppointmentReminderRepository;
import com.ocnilentes.mediagenda.modules.whatsapp.application.WhatsappPhoneNormalizer;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.exception.WhatsappApiException;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import com.ocnilentes.mediagenda.shared.config.ReminderProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReminderDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatchService.class);
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Bogota");

    private final ReminderProperties properties;
    private final AppointmentService appointmentService;
    private final AppointmentReminderRepository reminderRepository;
    private final WhatsappMessageClient whatsappMessageClient;

    public ReminderDispatchService(
            ReminderProperties properties,
            AppointmentService appointmentService,
            AppointmentReminderRepository reminderRepository,
            WhatsappMessageClient whatsappMessageClient
    ) {
        this.properties = properties;
        this.appointmentService = appointmentService;
        this.reminderRepository = reminderRepository;
        this.whatsappMessageClient = whatsappMessageClient;
    }

    public ReminderRunResult dispatchDueReminders() {
        return dispatchDueReminders(false);
    }

    public ReminderRunResult dispatchDueReminders(boolean ignoreWindow) {
        if (!properties.enabled()) {
            log.debug("Reminder dispatch skipped: reminder.enabled=false");
            return ReminderRunResult.disabled();
        }

        LocalDate today = LocalDate.now(CLINIC_ZONE);
        LocalTime now = LocalTime.now(CLINIC_ZONE);
        if (!ignoreWindow
                && (now.getHour() < properties.dayBeforeHour() || now.getHour() > properties.windowEndHour())) {
            log.debug(
                    "Reminder dispatch outside window hour={} expected={}..{}",
                    now.getHour(),
                    properties.dayBeforeHour(),
                    properties.windowEndHour()
            );
            return ReminderRunResult.outsideWindow();
        }

        LocalDate targetDate = today.plusDays(1);
        List<Appointment> appointments = appointmentService.listByDate(targetDate);

        int sent = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Appointment appointment : appointments) {
            ReminderChannel channel = ReminderPayloadFactory.channel();
            var existing = reminderRepository.findByAppointmentKeyAndChannel(appointment.id(), channel);
            if (existing.isPresent()) {
                ReminderStatus status = existing.get().status();
                if (status == ReminderStatus.SENT
                        || status == ReminderStatus.CONFIRMED
                        || status == ReminderStatus.CANCELLED) {
                    skipped++;
                    continue;
                }
            }

            String destination = destinationPhone(appointment.phone());
            if (destination == null) {
                skipped++;
                log.warn(
                        "Skipping reminder appointment={} phone='{}' (not a WhatsApp number; not sent to Meta)",
                        appointment.id(),
                        appointment.phone()
                );
                if (existing.isEmpty()) {
                    reminderRepository.save(invalidPhoneReminder(appointment, channel));
                }
                continue;
            }

            try {
                String metaMessageId = sendReminder(appointment, destination);
                AppointmentReminder reminder = new AppointmentReminder(
                        existing.map(AppointmentReminder::id).orElse(null),
                        appointment.id(),
                        channel,
                        destination,
                        appointment.patientName() == null ? "" : appointment.patientName(),
                        appointment.cedula(),
                        appointment.date(),
                        appointment.time(),
                        ReminderStatus.SENT,
                        metaMessageId,
                        null,
                        Instant.now(),
                        null
                );
                reminderRepository.save(reminder);
                sent++;
            } catch (Exception exception) {
                failed++;
                String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                errors.add(appointment.id() + ": " + message);
                log.error("Failed to send reminder for appointment={}", appointment.id(), exception);

                AppointmentReminder failedReminder = new AppointmentReminder(
                        existing.map(AppointmentReminder::id).orElse(null),
                        appointment.id(),
                        channel,
                        appointment.phone(),
                        appointment.patientName() == null ? "" : appointment.patientName(),
                        appointment.cedula(),
                        appointment.date(),
                        appointment.time(),
                        ReminderStatus.FAILED,
                        null,
                        truncate(message, 500),
                        null,
                        null
                );
                reminderRepository.save(failedReminder);
            }
        }

        log.info(
                "Reminder dispatch finished targetDate={} candidates={} sent={} skipped={} failed={}",
                targetDate,
                appointments.size(),
                sent,
                skipped,
                failed
        );
        return new ReminderRunResult(true, true, targetDate, appointments.size(), sent, skipped, failed, errors);
    }

    private String sendReminder(Appointment appointment, String destination) {
        if (properties.isTemplateMode()) {
            if (!StringUtils.hasText(properties.templateName())) {
                throw new IllegalStateException(
                        "reminder.template-name is required when reminder.send-mode=TEMPLATE"
                );
            }
            return whatsappMessageClient.sendTemplate(
                    destination,
                    properties.templateName(),
                    properties.templateLanguage(),
                    ReminderPayloadFactory.templateBodyParameters(appointment),
                    ReminderPayloadFactory.buttonsFor(appointment)
            );
        }

        try {
            return whatsappMessageClient.sendInteractiveButtons(
                    destination,
                    ReminderPayloadFactory.buildBody(appointment),
                    ReminderPayloadFactory.buttonsFor(appointment)
            );
        } catch (WhatsappApiException exception) {
            // Outside 24h window Meta rejects interactive; keep failure visible for TEMPLATE setup.
            throw exception;
        }
    }

    private static String destinationPhone(String rawPhone) {
        if (!WhatsappPhoneNormalizer.isLikelyPhone(rawPhone)) {
            return null;
        }
        return WhatsappPhoneNormalizer.normalize(rawPhone);
    }

    private AppointmentReminder invalidPhoneReminder(Appointment appointment, ReminderChannel channel) {
        return new AppointmentReminder(
                null,
                appointment.id(),
                channel,
                appointment.phone() == null ? "" : appointment.phone(),
                appointment.patientName() == null ? "" : appointment.patientName(),
                appointment.cedula(),
                appointment.date(),
                appointment.time(),
                ReminderStatus.FAILED,
                null,
                "Teléfono inválido; no se envió a Meta",
                null,
                null
        );
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record ReminderRunResult(
            boolean enabled,
            boolean insideWindow,
            LocalDate targetDate,
            int candidates,
            int sent,
            int skipped,
            int failed,
            List<String> errors
    ) {
        static ReminderRunResult disabled() {
            return new ReminderRunResult(false, false, null, 0, 0, 0, 0, List.of());
        }

        static ReminderRunResult outsideWindow() {
            return new ReminderRunResult(true, false, null, 0, 0, 0, 0, List.of());
        }
    }
}
