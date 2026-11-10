package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentService;
import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.information.application.ClinicInformationService;
import com.ocnilentes.mediagenda.modules.patient.application.CedulaValidator;
import com.ocnilentes.mediagenda.modules.patient.application.PatientNameValidator;
import com.ocnilentes.mediagenda.modules.patient.application.PatientService;
import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import com.ocnilentes.mediagenda.modules.patient.domain.model.Patient;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.exception.WhatsappApiException;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationIntent;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationSession;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationStep;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingTextMessage;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.InteractiveListRow;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ReplyButton;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private static final Locale SPANISH = Locale.of("es", "CO");
    private static final DateTimeFormatter DATE_LIST_TITLE =
            DateTimeFormatter.ofPattern("EEE d MMM", SPANISH);
    /** Horarios antes de esta hora se muestran como mañana; desde aquí, como tarde. */
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final int MAX_LIST_ROWS = 10;
    private static final int MAX_INVALID_ATTEMPTS = 3;
    private static final String PERIOD_PROMPT = "¿Deseas agendar la cita en la mañana o en la tarde?";

    private static final String MENU_BODY = """
            Hola, soy Ocni, el asistente de Ocnilentes.

            ¿Qué deseas hacer?""".trim();

    private static final String MENU_FALLBACK = """
            Hola, soy Ocni, el asistente de Ocnilentes.

            ¿Qué deseas hacer?
            1. Agendar cita
            2. Cancelar cita
            3. Otra información

            Responde con el número o pulsa una opción.""";

    private static final int INTERACTIVE_BODY_MAX = 1024;
    private static final List<ReplyButton> MENU_BUTTONS = List.of(
            new ReplyButton("1", "Agendar cita"),
            new ReplyButton("2", "Cancelar cita"),
            new ReplyButton("3", "Otra información")
    );
    private static final List<ReplyButton> MENU_ONLY_BUTTONS = List.of(
            new ReplyButton("menu", "Menú")
    );
    private static final List<ReplyButton> BACK_AND_MENU_BUTTONS = List.of(
            new ReplyButton("atras", "Atrás"),
            new ReplyButton("menu", "Menú")
    );
    private static final List<ReplyButton> RESCHEDULE_BUTTONS = List.of(
            new ReplyButton("1", "Sí"),
            new ReplyButton("2", "No"),
            new ReplyButton("menu", "Menú")
    );
    private static final List<ReplyButton> PERIOD_BUTTONS = List.of(
            new ReplyButton("1", "Mañana"),
            new ReplyButton("2", "Tarde"),
            new ReplyButton("menu", "Menú")
    );

    private static final String IDLE_EXPIRED_NOTICE = """
            Cerré la conversación anterior por inactividad (30 minutos).
            Así evitamos dejar el chat abierto y perder el hilo.

            """;

    private static final String TOO_MANY_INVALID = """
            Detecté 3 opciones inválidas. Cierro esta conversación para no dejar el chat abierto.

            """;

    private static final String TEMPORARY_FAILURE_MESSAGE = """
            Estamos teniendo una falla temporal con el asistente.
            Por favor vuelve a intentarlo en unos minutos.

            Si es urgente, comunícate directamente con la óptica.""".trim();

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

        String reply;
        try {
            reply = buildReply(message);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to build WhatsApp reply to={} messageId={}",
                    message.from(),
                    message.messageId(),
                    exception
            );
            sendBestEffort(message.from(), TEMPORARY_FAILURE_MESSAGE);
            return;
        }

        if (!StringUtils.hasText(reply)) {
            return;
        }

        log.info("Conversation reply to={} messageId={}", message.from(), message.messageId());
        try {
            whatsappMessageClient.sendTextMessage(message.from(), reply);
        } catch (WhatsappApiException exception) {
            log.error(
                    "Failed to deliver WhatsApp reply to={} messageId={} status={}",
                    message.from(),
                    message.messageId(),
                    exception.getStatusCode(),
                    exception
            );
            if (isAuthenticationFailure(exception)) {
                // Con token inválido Meta no acepta ningún mensaje: el usuario no puede ser avisado por WhatsApp.
                log.error(
                        "WhatsApp authentication failed (fix WHATSAPP_TOKEN). "
                                + "Cannot notify user {} about the outage via WhatsApp.",
                        message.from()
                );
                return;
            }
            sendBestEffort(message.from(), TEMPORARY_FAILURE_MESSAGE);
        }
    }

    private void sendBestEffort(String to, String body) {
        try {
            whatsappMessageClient.sendTextMessage(to, body);
        } catch (RuntimeException exception) {
            log.error("Also failed to send temporary-failure message to={}", to, exception);
        }
    }

    private boolean isAuthenticationFailure(WhatsappApiException exception) {
        int status = exception.getStatusCode();
        if (status == 401 || status == 403) {
            return true;
        }
        String body = exception.getResponseBody();
        return body != null && (body.contains("\"code\":190") || body.contains("\"code\": 190"));
    }

    private String buildReply(IncomingTextMessage message) {
        boolean expired = sessionStore.discardIfIdle(message.from());
        ConversationSession session = sessionStore.getOrCreate(message.from());
        String text = message.text() == null ? "" : message.text().trim();

        if (expired) {
            return offerMenu(session.phone(), IDLE_EXPIRED_NOTICE);
        }

        if (isMenuCommand(text)) {
            session.reset();
            return offerMenu(session.phone(), "");
        }

        if (session.step() != ConversationStep.IDLE) {
            return handleActiveFlow(session, text);
        }

        ConversationIntent intent = intentResolver.resolve(text);
        return switch (intent) {
            case SHOW_MENU -> offerMenu(session.phone(), "");
            case SCHEDULE -> startScheduling(session);
            case CANCEL -> startCancelling(session);
            case INFORMATION -> {
                String info = clinicInformationService.buildInformationMessage();
                yield sendWithButtons(session.phone(), info, MENU_ONLY_BUTTONS, info);
            }
        };
    }

    private String handleActiveFlow(ConversationSession session, String text) {
        return switch (session.step()) {
            case AWAITING_CONSENT -> {
                session.reset();
                yield offerMenu(session.phone(), "");
            }
            case AWAITING_SCHEDULE_CEDULA -> handleScheduleCedula(session, text);
            case AWAITING_CANCEL_CEDULA -> handleCancelCedula(session, text);
            case AWAITING_NAME -> handleName(session, text);
            case AWAITING_DATE_SELECTION -> handleDateSelection(session, text);
            case AWAITING_TIME_PERIOD_SELECTION -> handleTimePeriodSelection(session, text);
            case AWAITING_TIME_SELECTION -> handleTimeSelection(session, text);
            case AWAITING_CANCEL_SELECTION -> handleCancelSelection(session, text);
            case AWAITING_RESCHEDULE_DECISION -> handleRescheduleDecision(session, text);
            case IDLE -> offerMenu(session.phone(), "");
        };
    }

    private String offerMenu(String to, String intro) {
        String body = StringUtils.hasText(intro) ? intro.trim() + "\n\n" + MENU_BODY : MENU_BODY;
        return sendWithButtons(to, body, MENU_BUTTONS, StringUtils.hasText(intro)
                ? intro.trim() + "\n\n" + MENU_FALLBACK
                : MENU_FALLBACK);
    }

    private String sendWithMenu(String to, String body) {
        return sendWithButtons(to, body, MENU_ONLY_BUTTONS, body);
    }

    private String sendWithButtons(String to, String body, List<ReplyButton> buttons, String textFallback) {
        try {
            if (body != null && body.length() <= INTERACTIVE_BODY_MAX) {
                whatsappMessageClient.sendInteractiveButtons(to, body, buttons);
                return null;
            }
            whatsappMessageClient.sendTextMessage(to, body);
            whatsappMessageClient.sendInteractiveButtons(to, "Pulsa una opción.", buttons);
            return null;
        } catch (RuntimeException exception) {
            log.warn("Falling back to text options to={}", to, exception);
            return textFallback;
        }
    }

    private String startScheduling(ConversationSession session) {
        session.startScheduling();
        return "Perfecto, vamos a agendar tu cita.\n\n"
                + "¿Cuál es tu número de cédula? Escríbela solo con números, sin puntos ni comas.";
    }

    private String startCancelling(ConversationSession session) {
        session.startCancellingAskCedula();
        return "Vamos a cancelar una cita.\n\n"
                + "¿Cuál es el número de cédula del paciente? Escríbela solo con números, sin puntos ni comas.";
    }

    private String handleScheduleCedula(ConversationSession session, String text) {
        String cedula;
        try {
            cedula = CedulaValidator.normalizeAndValidate(text);
        } catch (PatientException exception) {
            return rejectInvalid(session, exception.getMessage());
        }

        Optional<Patient> existing = patientService.findByCedula(cedula);
        if (existing.isPresent()) {
            List<LocalDate> dates = appointmentService.listAvailableDates();
            if (dates.isEmpty()) {
                session.reset();
                return sendWithMenu(session.phone(), "No hay fechas disponibles en este momento.");
            }
            Patient patient = existing.get();
            patientService.touchPhone(cedula, session.phone());
            session.knownPatientReady(cedula, patient.fullName(), dates);
            return offerDateOptions(session.phone(), "Hola, " + patient.fullName() + ".\n\n", dates);
        }

        session.cedulaReceivedForNewPatient(cedula);
        return "Gracias. No te tenemos registrado aún.\n\n"
                + "¿Cuál es tu nombre y apellido? Solo letras, sin notas, máximo 40 caracteres.";
    }

    private String handleCancelCedula(ConversationSession session, String text) {
        String cedula;
        try {
            cedula = CedulaValidator.normalizeAndValidate(text);
        } catch (PatientException exception) {
            return rejectInvalid(session, exception.getMessage());
        }

        List<Appointment> appointments = appointmentService.listByCedula(cedula);
        if (appointments.isEmpty()) {
            session.reset();
            return sendWithMenu(session.phone(), "No encontramos citas para esa cédula.");
        }

        session.startCancelling(cedula, appointments.stream().map(Appointment::id).toList());
        return offerCancelOptions(session.phone(), appointments);
    }

    private String handleName(ConversationSession session, String text) {
        String name;
        try {
            name = PatientNameValidator.normalizeAndValidate(text);
        } catch (PatientException exception) {
            return rejectInvalid(session, exception.getMessage());
        }

        List<LocalDate> dates = appointmentService.listAvailableDates();
        if (dates.isEmpty()) {
            session.reset();
            return sendWithMenu(session.phone(), "No hay fechas disponibles en este momento.");
        }

        patientService.registerOrUpdate(session.cedula(), name, session.phone());
        session.nameReceived(name, dates);
        return offerDateOptions(session.phone(), "Gracias, " + name + ".\n\n", dates);
    }

    private String handleDateSelection(ConversationSession session, String text) {
        Integer selected = parseOption(text, session.dateOptions().size());
        if (selected == null) {
            return rejectInvalid(
                    session,
                    offerDateOptions(session.phone(), "Opción inválida.\n\n", session.dateOptions()),
                    "Opción inválida. Responde con el número de la fecha."
            );
        }

        LocalDate date = session.dateOptions().get(selected - 1);
        Optional<Appointment> existingSameDay =
                appointmentService.findByCedulaAndDate(session.cedula(), date);
        if (existingSameDay.isPresent()) {
            Appointment existing = existingSameDay.get();
            return offerDateOptions(
                    session.phone(),
                    "😊 Ya tienes una cita reservada para ese día a las "
                            + existing.time().format(TIME_FORMAT)
                            + ". Para poder atenderte de la mejor manera, solo puedes tener una cita por día.\n\n"
                            + "Puedes elegir otra fecha con gusto:\n\n",
                    session.dateOptions()
            );
        }

        List<LocalTime> times = appointmentService.listAvailableTimes(date);
        if (times.isEmpty()) {
            return offerDateOptions(
                    session.phone(),
                    "Esa fecha ya no tiene horarios. Elige otra:\n\n",
                    session.dateOptions()
            );
        }

        session.dateSelected(date, times);
        return promptAfterDateSelected(session);
    }

    private String handleTimePeriodSelection(ConversationSession session, String text) {
        List<LocalTime> morning = morningTimes(session.availableTimes());
        List<LocalTime> afternoon = afternoonTimes(session.availableTimes());

        if (morning.isEmpty() && afternoon.isEmpty()) {
            session.reset();
            return sendWithMenu(session.phone(), "Ya no hay horarios para esa fecha.");
        }

        // Si solo queda una franja (p. ej. se llenó la otra), saltamos a horarios.
        if (morning.isEmpty() || afternoon.isEmpty()) {
            List<LocalTime> only = morning.isEmpty() ? afternoon : morning;
            String label = morning.isEmpty() ? "tarde" : "mañana";
            session.timePeriodSelected(only);
            return offerTimeOptions(session, only, label, false);
        }

        Integer selected = parsePeriodOption(text);
        if (selected == null) {
            return rejectInvalid(
                    session,
                    offerPeriodOptions(
                            session.phone(),
                            "Opción inválida.\n\n",
                            session.selectedDate(),
                            morning,
                            afternoon
                    ),
                    "Opción inválida. Responde 1 para mañana o 2 para tarde."
            );
        }

        List<LocalTime> chosen = selected == 1 ? morning : afternoon;
        String label = selected == 1 ? "mañana" : "tarde";
        session.timePeriodSelected(chosen);
        return offerTimeOptions(session, chosen, label, true);
    }

    private String handleTimeSelection(ConversationSession session, String text) {
        if (isBackCommand(text) && hasBothPeriods(session.availableTimes())) {
            session.backToTimePeriodSelection();
            return offerPeriodOptions(
                    session.phone(),
                    "",
                    session.selectedDate(),
                    morningTimes(session.availableTimes()),
                    afternoonTimes(session.availableTimes())
            );
        }

        Integer selected = parseOption(text, session.timeOptions().size());
        if (selected == null) {
            boolean canGoBack = hasBothPeriods(session.availableTimes());
            String label = inferPeriodLabel(session.timeOptions());
            return rejectInvalid(
                    session,
                    offerTimeOptions(session, session.timeOptions(), label, canGoBack),
                    "Opción inválida. Responde con el número de la hora."
            );
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
            return sendWithMenu(session.phone(), """
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
                    """.formatted(
                    appointment.patientName(),
                    appointment.cedula(),
                    appointment.date().format(DATE_FORMAT),
                    appointment.time().format(TIME_FORMAT)
            ).trim());
        } catch (AppointmentException exception) {
            session.reset();
            return sendWithMenu(session.phone(), exception.getMessage());
        }
    }

    private String handleCancelSelection(ConversationSession session, String text) {
        Integer selected = parseOption(text, session.cancelCandidateIds().size());
        if (selected == null) {
            return rejectInvalid(session, "Opción inválida. Responde con el número de la cita (ejemplo: 1).");
        }

        try {
            String appointmentId = session.cancelCandidateIds().get(selected - 1);
            Appointment cancelled = appointmentService.cancel(appointmentId, session.cedula());
            session.awaitRescheduleDecision(cancelled.patientName(), cancelled.cedula());
            return sendWithButtons(
                    session.phone(),
                    """
                            ✅ Cita cancelada.

                            Fecha: %s
                            Hora: %s

                            ¿Deseas reagendar?
                            """.formatted(
                            cancelled.date().format(DATE_FORMAT),
                            cancelled.time().format(TIME_FORMAT)
                    ).trim(),
                    RESCHEDULE_BUTTONS,
                    """
                            ✅ Cita cancelada.

                            Fecha: %s
                            Hora: %s

                            ¿Deseas reagendar?
                            1. Sí
                            2. No
                            """.formatted(
                            cancelled.date().format(DATE_FORMAT),
                            cancelled.time().format(TIME_FORMAT)
                    ).trim()
            );
        } catch (AppointmentException exception) {
            session.reset();
            return sendWithMenu(session.phone(), exception.getMessage());
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
                return rejectInvalid(
                        session,
                        sendWithButtons(
                                session.phone(),
                                "Opción inválida.\n\n¿Deseas reagendar?",
                                RESCHEDULE_BUTTONS,
                                """
                                        Opción inválida.

                                        ¿Deseas reagendar?
                                        1. Sí
                                        2. No
                                        """.trim()
                        ),
                        "Opción inválida. Pulsa Sí o No."
                );
            }
        }

        if (selected == 2) {
            session.clearAfterCompletion();
            return sendWithMenu(session.phone(), "Listo. Cuando necesites algo más, pulsa Menú.");
        }

        return startRescheduling(session);
    }

    private String startRescheduling(ConversationSession session) {
        List<LocalDate> dates = appointmentService.listAvailableDates();
        if (dates.isEmpty()) {
            session.reset();
            return sendWithMenu(session.phone(), "No hay fechas disponibles para reagendar en este momento.");
        }

        session.knownPatientReady(session.cedula(), session.patientName(), dates);
        return offerDateOptions(
                session.phone(),
                "Perfecto, " + session.patientName() + ". Vamos a reagendar tu cita.\n\n",
                dates
        );
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

    private String promptAfterDateSelected(ConversationSession session) {
        List<LocalTime> morning = morningTimes(session.availableTimes());
        List<LocalTime> afternoon = afternoonTimes(session.availableTimes());
        String header = "Fecha: " + session.selectedDate().format(DATE_FORMAT) + "\n\n";

        if (!morning.isEmpty() && afternoon.isEmpty()) {
            session.timePeriodSelected(morning);
            return offerTimeOptions(session, morning, "mañana", false);
        }
        if (morning.isEmpty() && !afternoon.isEmpty()) {
            session.timePeriodSelected(afternoon);
            return offerTimeOptions(session, afternoon, "tarde", false);
        }
        return offerPeriodOptions(session.phone(), "", session.selectedDate(), morning, afternoon);
    }

    private String formatPeriodOptions(List<LocalTime> morning, List<LocalTime> afternoon) {
        return """
                ¿Deseas agendar la cita en la mañana o en la tarde?

                1. Mañana (%s - %s)
                2. Tarde (%s - %s)

                Responde con el número de la opción.
                """.formatted(
                morning.getFirst().format(TIME_FORMAT),
                morning.getLast().format(TIME_FORMAT),
                afternoon.getFirst().format(TIME_FORMAT),
                afternoon.getLast().format(TIME_FORMAT)
        ).trim();
    }

    private String formatTimeOptions(List<LocalTime> times, String periodLabel) {
        StringBuilder builder = new StringBuilder("Horarios de la ")
                .append(periodLabel)
                .append(":\n\n");
        for (int index = 0; index < times.size(); index++) {
            builder.append(index + 1)
                    .append(". ")
                    .append(times.get(index).format(TIME_FORMAT))
                    .append("\n");
        }
        builder.append("\nResponde con el número de la opción.");
        return builder.toString();
    }

    private String offerTimeOptions(
            ConversationSession session,
            List<LocalTime> times,
            String periodLabel,
            boolean offerBack
    ) {
        String header = "Fecha: " + session.selectedDate().format(DATE_FORMAT) + "\n\n";
        String body = header + formatTimeOptions(times, periodLabel);
        List<ReplyButton> buttons = offerBack ? BACK_AND_MENU_BUTTONS : MENU_ONLY_BUTTONS;
        try {
            whatsappMessageClient.sendTextMessage(session.phone(), body);
            whatsappMessageClient.sendInteractiveButtons(
                    session.phone(),
                    offerBack
                            ? "Elige el número de la hora, o pulsa Atrás / Menú."
                            : "Elige el número de la hora, o pulsa Menú.",
                    buttons
            );
            return null;
        } catch (RuntimeException exception) {
            log.warn("Falling back to numbered time options to={}", session.phone(), exception);
            return body + (offerBack ? "\nEscribe *atrás* para cambiar de franja." : "");
        }
    }

    private String offerCancelOptions(String to, List<Appointment> appointments) {
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
        String body = builder.toString();

        if (appointments.size() <= 3) {
            List<ReplyButton> buttons = new ArrayList<>();
            for (int index = 0; index < appointments.size(); index++) {
                Appointment appointment = appointments.get(index);
                buttons.add(new ReplyButton(
                        String.valueOf(index + 1),
                        clip(appointment.date().format(DATE_FORMAT) + " "
                                + appointment.time().format(TIME_FORMAT), 20)
                ));
            }
            if (buttons.size() < 3) {
                buttons.add(new ReplyButton("menu", "Menú"));
            }
            return sendWithButtons(to, body, buttons, body);
        }
        return sendWithMenu(to, body);
    }

    private List<LocalTime> morningTimes(List<LocalTime> times) {
        return times.stream().filter(time -> time.isBefore(AFTERNOON_START)).toList();
    }

    private List<LocalTime> afternoonTimes(List<LocalTime> times) {
        return times.stream().filter(time -> !time.isBefore(AFTERNOON_START)).toList();
    }

    private boolean hasBothPeriods(List<LocalTime> times) {
        return !morningTimes(times).isEmpty() && !afternoonTimes(times).isEmpty();
    }

    private String inferPeriodLabel(List<LocalTime> times) {
        if (times.isEmpty()) {
            return "día";
        }
        return times.getFirst().isBefore(AFTERNOON_START) ? "mañana" : "tarde";
    }

    private String offerDateOptions(String to, String intro, List<LocalDate> dates) {
        String body = intro + "Elige una fecha disponible. Pulsa *Ver fechas* y selecciona el día.";
        try {
            whatsappMessageClient.sendInteractiveList(
                    to,
                    body,
                    "Ver fechas",
                    "Disponibles",
                    dateListRows(dates)
            );
            whatsappMessageClient.sendInteractiveButtons(
                    to,
                    "Si prefieres otra opción, pulsa Menú.",
                    MENU_ONLY_BUTTONS
            );
            return null;
        } catch (RuntimeException exception) {
            log.warn("Falling back to numbered date options to={}", to, exception);
            return intro + formatDateOptions(dates);
        }
    }

    private String offerPeriodOptions(
            String to,
            String intro,
            LocalDate date,
            List<LocalTime> morning,
            List<LocalTime> afternoon
    ) {
        String header = "Fecha: " + date.format(DATE_FORMAT) + "\n\n";
        String body = intro + header
                + PERIOD_PROMPT + "\n\n"
                + "Mañana " + morning.getFirst().format(TIME_FORMAT)
                + "–" + morning.getLast().format(TIME_FORMAT)
                + " · Tarde " + afternoon.getFirst().format(TIME_FORMAT)
                + "–" + afternoon.getLast().format(TIME_FORMAT);
        try {
            whatsappMessageClient.sendInteractiveButtons(
                    to,
                    body,
                    PERIOD_BUTTONS
            );
            return null;
        } catch (RuntimeException exception) {
            log.warn("Falling back to numbered period options to={}", to, exception);
            return intro + header + formatPeriodOptions(morning, afternoon);
        }
    }

    private List<InteractiveListRow> dateListRows(List<LocalDate> dates) {
        int limit = Math.min(dates.size(), MAX_LIST_ROWS);
        List<InteractiveListRow> rows = new ArrayList<>(limit);
        for (int index = 0; index < limit; index++) {
            LocalDate date = dates.get(index);
            rows.add(new InteractiveListRow(
                    String.valueOf(index + 1),
                    clip(capitalize(date.format(DATE_LIST_TITLE)), 24),
                    date.format(DATE_FORMAT)
            ));
        }
        return rows;
    }

    private static String capitalize(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String clip(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String rejectInvalid(ConversationSession session, String retryMessage) {
        return rejectInvalid(session, retryMessage, retryMessage);
    }

    private String rejectInvalid(ConversationSession session, String retryMessage, String fallbackText) {
        session.registerInvalidAttempt();
        if (session.invalidAttempts() >= MAX_INVALID_ATTEMPTS) {
            session.reset();
            return offerMenu(session.phone(), TOO_MANY_INVALID);
        }
        if (StringUtils.hasText(retryMessage)) {
            return retryMessage;
        }
        return fallbackText;
    }

    private Integer parsePeriodOption(String text) {
        Integer selected = parseOption(text, 2);
        if (selected != null) {
            return selected;
        }
        String normalized = text == null ? "" : text.trim().toLowerCase(SPANISH)
                .replace("á", "a");
        if (normalized.equals("manana") || normalized.equals("m")) {
            return 1;
        }
        if (normalized.equals("tarde") || normalized.equals("t")) {
            return 2;
        }
        return null;
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

    private boolean isBackCommand(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("atras")
                || normalized.equals("atrás")
                || normalized.equals("volver");
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
