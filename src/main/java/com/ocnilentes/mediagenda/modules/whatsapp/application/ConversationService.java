package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentService;
import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.information.application.ClinicInformationService;
import com.ocnilentes.mediagenda.modules.patient.application.CedulaValidator;
import com.ocnilentes.mediagenda.modules.patient.application.PatientService;
import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import com.ocnilentes.mediagenda.modules.patient.domain.model.Patient;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationIntent;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationSession;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationStep;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingTextMessage;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String MENU = """
            ¡Hola! Soy Ocnielentes, tu asistente.

            ¿Qué deseas hacer?
            1. Agendar
            2. Cancelar
            3. Información

            Responde con el número de la opción.""";

    private final ConversationIntentResolver intentResolver;
    private final ConversationSessionStore sessionStore;
    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final ClinicInformationService clinicInformationService;
    private final WhatsappMessageClient whatsappMessageClient;
    private final Set<String> processedMessageIds = ConcurrentHashMap.newKeySet();

    public ConversationService(
            ConversationIntentResolver intentResolver,
            ConversationSessionStore sessionStore,
            AppointmentService appointmentService,
            PatientService patientService,
            ClinicInformationService clinicInformationService,
            WhatsappMessageClient whatsappMessageClient
    ) {
        this.intentResolver = intentResolver;
        this.sessionStore = sessionStore;
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.clinicInformationService = clinicInformationService;
        this.whatsappMessageClient = whatsappMessageClient;
    }

    public void handle(IncomingTextMessage message) {
        if (message == null || !StringUtils.hasText(message.from())) {
            log.warn(
                    "Skipping WhatsApp message without sender phone id={}",
                    message == null ? null : message.messageId()
            );
            return;
        }

        if (!processedMessageIds.add(message.messageId())) {
            log.debug("Skipping already processed message id={}", message.messageId());
            return;
        }

        String reply = buildReply(message);
        log.info("Conversation reply to={} messageId={}", message.from(), message.messageId());
        whatsappMessageClient.sendTextMessage(message.from(), reply);
    }

    private String buildReply(IncomingTextMessage message) {
        ConversationSession session = sessionStore.getOrCreate(message.from());
        String text = message.text() == null ? "" : message.text().trim();

        if (isMenuCommand(text)) {
            session.reset();
            return MENU;
        }

        if (session.step() != ConversationStep.IDLE) {
            return handleActiveFlow(session, text);
        }

        ConversationIntent intent = intentResolver.resolve(text);
        return switch (intent) {
            case SHOW_MENU -> MENU;
            case SCHEDULE -> startScheduling(session);
            case CANCEL -> startCancelling(session);
            case INFORMATION -> clinicInformationService.buildInformationMessage();
        };
    }

    private String handleActiveFlow(ConversationSession session, String text) {
        return switch (session.step()) {
            case AWAITING_SCHEDULE_CEDULA -> handleScheduleCedula(session, text);
            case AWAITING_CANCEL_CEDULA -> handleCancelCedula(session, text);
            case AWAITING_NAME -> handleName(session, text);
            case AWAITING_DATE_SELECTION -> handleDateSelection(session, text);
            case AWAITING_TIME_SELECTION -> handleTimeSelection(session, text);
            case AWAITING_CANCEL_SELECTION -> handleCancelSelection(session, text);
            case AWAITING_RESCHEDULE_DECISION -> handleRescheduleDecision(session, text);
            case IDLE -> MENU;
        };
    }

    private String startScheduling(ConversationSession session) {
        session.startScheduling();
        return "Perfecto, vamos a agendar tu cita.\n\n¿Cuál es tu número de cédula?";
    }

    private String startCancelling(ConversationSession session) {
        session.startCancellingAskCedula();
        return "Vamos a cancelar una cita.\n\n¿Cuál es el número de cédula del paciente?";
    }

    private String handleScheduleCedula(ConversationSession session, String text) {
        String cedula;
        try {
            cedula = CedulaValidator.normalizeAndValidate(text);
        } catch (PatientException exception) {
            return exception.getMessage();
        }

        Optional<Patient> existing = patientService.findByCedula(cedula);
        if (existing.isPresent()) {
            List<LocalDate> dates = appointmentService.listAvailableDates();
            if (dates.isEmpty()) {
                session.reset();
                return "No hay fechas disponibles en este momento.\n\nEscribe *menú* para volver.";
            }
            Patient patient = existing.get();
            patientService.touchPhone(cedula, session.phone());
            session.knownPatientReady(cedula, patient.fullName(), dates);
            return "Hola, " + patient.fullName() + ".\n\n" + formatDateOptions(dates);
        }

        session.cedulaReceivedForNewPatient(cedula);
        return "Gracias. No te tenemos registrado aún.\n\n¿Cuál es tu nombre completo?";
    }

    private String handleCancelCedula(ConversationSession session, String text) {
        String cedula;
        try {
            cedula = CedulaValidator.normalizeAndValidate(text);
        } catch (PatientException exception) {
            return exception.getMessage();
        }

        List<Appointment> appointments = appointmentService.listByCedula(cedula);
        if (appointments.isEmpty()) {
            session.reset();
            return "No encontramos citas para esa cédula.\n\nEscribe *menú* para ver opciones.";
        }

        session.startCancelling(cedula, appointments.stream().map(Appointment::id).toList());

        StringBuilder builder = new StringBuilder("Estas son las citas encontradas:\n\n");
        for (int index = 0; index < appointments.size(); index++) {
            Appointment appointment = appointments.get(index);
            builder.append(index + 1)
                    .append(". ")
                    .append(appointment.date().format(DATE_FORMAT))
                    .append(" ")
                    .append(appointment.time().format(TIME_FORMAT))
                    .append(" - ")
                    .append(appointment.patientName())
                    .append(" (Consultorio ")
                    .append(appointment.consultorio().name())
                    .append(")\n");
        }
        builder.append("\nResponde con el número de la cita que deseas cancelar.");
        return builder.toString();
    }

    private String handleName(ConversationSession session, String text) {
        if (!StringUtils.hasText(text) || text.length() < 2) {
            return "Por favor escribe tu nombre completo.";
        }

        List<LocalDate> dates = appointmentService.listAvailableDates();
        if (dates.isEmpty()) {
            session.reset();
            return "No hay fechas disponibles en este momento.\n\nEscribe *menú* para volver.";
        }

        patientService.registerOrUpdate(session.cedula(), text, session.phone());
        session.nameReceived(text, dates);
        return "Gracias, " + text + ".\n\n" + formatDateOptions(dates);
    }

    private String handleDateSelection(ConversationSession session, String text) {
        Integer selected = parseOption(text, session.dateOptions().size());
        if (selected == null) {
            return "Opción inválida.\n\n" + formatDateOptions(session.dateOptions());
        }

        LocalDate date = session.dateOptions().get(selected - 1);
        Optional<Appointment> existingSameDay = appointmentService.listByCedula(session.cedula()).stream()
                .filter(appointment -> appointment.date().equals(date))
                .findFirst();
        if (existingSameDay.isPresent()) {
            Appointment existing = existingSameDay.get();
            return "😊 Ya tienes una cita reservada para ese día a las "
                    + existing.time().format(TIME_FORMAT)
                    + ". Para poder atenderte de la mejor manera, solo puedes tener una cita por día.\n\n"
                    + "Puedes elegir otra fecha con gusto:\n\n"
                    + formatDateOptions(session.dateOptions());
        }

        List<LocalTime> times = appointmentService.listAvailableTimes(date);
        if (times.isEmpty()) {
            return "Esa fecha ya no tiene horarios. Elige otra:\n\n" + formatDateOptions(session.dateOptions());
        }

        session.dateSelected(date, times);
        return "Fecha: " + date.format(DATE_FORMAT) + "\n\n" + formatTimeOptions(times);
    }

    private String handleTimeSelection(ConversationSession session, String text) {
        Integer selected = parseOption(text, session.timeOptions().size());
        if (selected == null) {
            return "Opción inválida.\n\n" + formatTimeOptions(session.timeOptions());
        }

        LocalTime time = session.timeOptions().get(selected - 1);
        try {
            Appointment appointment = appointmentService.schedule(
                    session.phone(),
                    session.patientName(),
                    session.cedula(),
                    session.selectedDate(),
                    time
            );
            patientService.registerOrUpdate(
                    session.cedula(),
                    session.patientName(),
                    session.phone()
            );
            session.clearAfterCompletion();
            return """
                    ✅ ¡Cita agendada con éxito!

                    👤 Nombre: %s
                    🪪 Cédula: %s
                    📅 Fecha: %s
                    🕐 Hora: %s

                    ⏰ Te recomendamos llegar con 10 minutos de anticipación para no correr el riesgo de perder tu cita.

                    📋 Ten en cuenta:
                    - Si sufres de diabetes, asiste con tus valores de glicemia estables.
                    - Si usas lentes de contacto, retíralos mínimo 12 horas antes.
                    - Si estás en embarazo, no se recomienda realizar el examen de optometría.

                    ¡Gracias por elegirnos! 💙
                    Escribe *menú* para más opciones.
                    """.formatted(
                    appointment.patientName(),
                    appointment.cedula(),
                    appointment.date().format(DATE_FORMAT),
                    appointment.time().format(TIME_FORMAT)
            ).trim();
        } catch (AppointmentException exception) {
            session.reset();
            return exception.getMessage() + "\n\nEscribe *menú* para volver a intentar.";
        }
    }

    private String handleCancelSelection(ConversationSession session, String text) {
        Integer selected = parseOption(text, session.cancelCandidateIds().size());
        if (selected == null) {
            return "Opción inválida. Responde con el número de la cita (ejemplo: 1).";
        }

        try {
            String appointmentId = session.cancelCandidateIds().get(selected - 1);
            Appointment cancelled = appointmentService.cancel(appointmentId, session.cedula());
            session.awaitRescheduleDecision(cancelled.patientName(), cancelled.cedula());
            return """
                    ✅ Cita cancelada.

                    Fecha: %s
                    Hora: %s

                    ¿Deseas reagendar?
                    1. Sí
                    2. No
                    """.formatted(
                    cancelled.date().format(DATE_FORMAT),
                    cancelled.time().format(TIME_FORMAT)
            ).trim();
        } catch (AppointmentException exception) {
            session.reset();
            return exception.getMessage() + "\n\nEscribe *menú* para volver.";
        }
    }

    private String handleRescheduleDecision(ConversationSession session, String text) {
        Integer selected = parseOption(text, 2);
        if (selected == null) {
            String normalized = text.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("si") || normalized.equals("sí") || normalized.equals("s")) {
                selected = 1;
            } else if (normalized.equals("no") || normalized.equals("n")) {
                selected = 2;
            } else {
                return """
                        Opción inválida.

                        ¿Deseas reagendar?
                        1. Sí
                        2. No
                        """.trim();
            }
        }

        if (selected == 2) {
            session.clearAfterCompletion();
            return "Listo. Escribe *menú* cuando necesites algo más.";
        }

        return startRescheduling(session);
    }

    private String startRescheduling(ConversationSession session) {
        List<LocalDate> dates = appointmentService.listAvailableDates();
        if (dates.isEmpty()) {
            session.reset();
            return "No hay fechas disponibles para reagendar en este momento.\n\nEscribe *menú* para volver.";
        }

        session.knownPatientReady(session.cedula(), session.patientName(), dates);
        return "Perfecto, " + session.patientName() + ". Vamos a reagendar tu cita.\n\n"
                + formatDateOptions(dates);
    }

    private String formatDateOptions(List<LocalDate> dates) {
        StringBuilder builder = new StringBuilder("Elige una fecha disponible:\n\n");
        for (int index = 0; index < dates.size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(dates.get(index).format(DATE_FORMAT))
                    .append("\n");
        }
        builder.append("\nResponde con el número de la opción.");
        return builder.toString();
    }

    private String formatTimeOptions(List<LocalTime> times) {
        StringBuilder builder = new StringBuilder("Elige un horario disponible:\n\n");
        for (int index = 0; index < times.size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(times.get(index).format(TIME_FORMAT))
                    .append("\n");
        }
        builder.append("\nResponde con el número de la opción.");
        return builder.toString();
    }

    private Integer parseOption(String text, int max) {
        try {
            int selected = Integer.parseInt(text.trim());
            if (selected < 1 || selected > max) {
                return null;
            }
            return selected;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isMenuCommand(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("menu")
                || normalized.equals("menú")
                || normalized.equals("0")
                || normalized.equals("inicio");
    }
}
