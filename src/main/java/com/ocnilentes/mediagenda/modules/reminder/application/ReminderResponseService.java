package com.ocnilentes.mediagenda.modules.reminder.application;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentService;
import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.AppointmentReminder;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import com.ocnilentes.mediagenda.modules.reminder.domain.port.AppointmentReminderRepository;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingButtonReply;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReminderResponseService {

    private static final Logger log = LoggerFactory.getLogger(ReminderResponseService.class);

    private final AppointmentReminderRepository reminderRepository;
    private final AppointmentService appointmentService;
    private final WhatsappMessageClient whatsappMessageClient;

    public ReminderResponseService(
            AppointmentReminderRepository reminderRepository,
            AppointmentService appointmentService,
            WhatsappMessageClient whatsappMessageClient
    ) {
        this.reminderRepository = reminderRepository;
        this.appointmentService = appointmentService;
        this.whatsappMessageClient = whatsappMessageClient;
    }

    public boolean handle(IncomingButtonReply reply) {
        if (reply == null || !StringUtils.hasText(reply.buttonId()) && !StringUtils.hasText(reply.buttonTitle())) {
            return false;
        }

        String buttonId = reply.buttonId() == null ? "" : reply.buttonId().trim();
        String buttonTitle = reply.buttonTitle() == null ? "" : reply.buttonTitle().trim();

        boolean confirm = ReminderPayloadFactory.isConfirm(buttonId)
                || ReminderPayloadFactory.isConfirm(buttonTitle);
        boolean cancel = ReminderPayloadFactory.isCancel(buttonId)
                || ReminderPayloadFactory.isCancel(buttonTitle);

        if (!confirm && !cancel) {
            return false;
        }

        Optional<AppointmentReminder> reminder = resolveReminder(reply.from(), buttonId);
        if (reminder.isEmpty()) {
            whatsappMessageClient.sendTextMessage(
                    reply.from(),
                    "No encontramos un recordatorio pendiente asociado a esta cita.\n\nEscribe *menú* para ver opciones."
            );
            return true;
        }

        AppointmentReminder current = reminder.get();
        if (current.status() == ReminderStatus.CONFIRMED || current.status() == ReminderStatus.CANCELLED) {
            whatsappMessageClient.sendTextMessage(
                    reply.from(),
                    "Esta cita ya fue " + (current.status() == ReminderStatus.CONFIRMED ? "confirmada" : "cancelada") + "."
            );
            return true;
        }

        if (confirm) {
            try {
                appointmentService.markConfirmed(current.appointmentKey(), reply.from());
            } catch (AppointmentException exception) {
                log.warn(
                        "Could not mark CONFIRMO in sheet for appointmentKey={}: {}",
                        current.appointmentKey(),
                        exception.getMessage()
                );
            }
            reminderRepository.save(withStatus(current, ReminderStatus.CONFIRMED));
            whatsappMessageClient.sendTextMessage(
                    reply.from(),
                    """
                            ✅ Cita confirmada.

                            Fecha: %s
                            Hora: %s

                            Te esperamos. Llega 10 minutos antes.
                            """.formatted(
                            ReminderPayloadFactory.formatDate(current.appointmentDate()),
                            ReminderPayloadFactory.formatTime(current.appointmentTime())
                    ).trim()
            );
            log.info("Reminder confirmed appointmentKey={} phone={}", current.appointmentKey(), reply.from());
            return true;
        }

        try {
            Appointment cancelled = appointmentService.cancelByPhone(current.appointmentKey(), reply.from());
            reminderRepository.save(withStatus(current, ReminderStatus.CANCELLED));
            whatsappMessageClient.sendTextMessage(
                    reply.from(),
                    """
                            ✅ Cita cancelada.

                            Fecha: %s
                            Hora: %s

                            Si deseas agendar otra, escribe *menú*.
                            """.formatted(
                            ReminderPayloadFactory.formatDate(cancelled.date()),
                            ReminderPayloadFactory.formatTime(cancelled.time())
                    ).trim()
            );
            log.info("Reminder cancelled appointmentKey={} phone={}", current.appointmentKey(), reply.from());
        } catch (AppointmentException exception) {
            whatsappMessageClient.sendTextMessage(
                    reply.from(),
                    exception.getMessage() + "\n\nEscribe *menú* para cancelar desde el menú."
            );
        }
        return true;
    }

    private Optional<AppointmentReminder> resolveReminder(String phone, String buttonId) {
        String appointmentKey = ReminderPayloadFactory.extractAppointmentKey(buttonId);
        if (StringUtils.hasText(appointmentKey)) {
            return reminderRepository.findByAppointmentKeyAndChannel(
                    appointmentKey,
                    ReminderPayloadFactory.channel()
            );
        }

        List<AppointmentReminder> open = reminderRepository.findByPhoneAndStatusIn(
                phone,
                List.of(ReminderStatus.SENT, ReminderStatus.FAILED)
        );
        // Preferir el recordatorio más reciente en SENT (no el más viejo).
        return open.stream()
                .sorted(Comparator
                        .comparing((AppointmentReminder reminder) -> reminder.status() == ReminderStatus.SENT ? 0 : 1)
                        .thenComparing(AppointmentReminder::appointmentDate, Comparator.reverseOrder())
                        .thenComparing(AppointmentReminder::appointmentTime, Comparator.reverseOrder()))
                .findFirst();
    }

    private AppointmentReminder withStatus(AppointmentReminder current, ReminderStatus status) {
        return new AppointmentReminder(
                current.id(),
                current.appointmentKey(),
                current.channel(),
                current.phone(),
                current.patientName(),
                current.cedula(),
                current.appointmentDate(),
                current.appointmentTime(),
                status,
                current.metaMessageId(),
                current.errorMessage(),
                current.sentAt(),
                Instant.now()
        );
    }
}
