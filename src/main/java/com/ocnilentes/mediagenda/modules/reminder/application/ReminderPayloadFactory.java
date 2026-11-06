package com.ocnilentes.mediagenda.modules.reminder.application;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ReplyButton;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;

public final class ReminderPayloadFactory {

    public static final String CONFIRM_PREFIX = "rc|";
    public static final String CANCEL_PREFIX = "rx|";
    static final String CLINIC_PLACE =
            "Calle 15#8-103, Centro comercial Petecuy, Local 119, Torre A";

    private static final Locale SPANISH = Locale.of("es", "CO");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH);

    private ReminderPayloadFactory() {
    }

    public static String confirmButtonId(String appointmentKey) {
        return CONFIRM_PREFIX + appointmentKey;
    }

    public static String cancelButtonId(String appointmentKey) {
        return CANCEL_PREFIX + appointmentKey;
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT).toLowerCase(SPANISH);
    }

    public static String formatTime(LocalTime time) {
        int hour = time.get(ChronoField.CLOCK_HOUR_OF_AMPM);
        String period = time.get(ChronoField.AMPM_OF_DAY) == 0 ? "a. m." : "p. m.";
        return hour + ":" + String.format("%02d", time.getMinute()) + " " + period;
    }

    public static String buildBody(Appointment appointment) {
        return """
                Hola, %s 😊 Queremos recordarte que tienes cita de optometría con nosotros:

                📅 Fecha: %s
                🕘 Hora: %s
                📍 Lugar: %s

                Por favor, cuéntanos si podrás asistir usando los botones de abajo. ¡Gracias!
                """.formatted(
                patientName(appointment),
                formatDate(appointment.date()),
                formatTime(appointment.time()),
                CLINIC_PLACE
        ).trim();
    }

    public static List<String> templateBodyParameters(Appointment appointment) {
        return List.of(
                patientName(appointment),
                formatDate(appointment.date()),
                formatTime(appointment.time())
        );
    }

    public static List<ReplyButton> buttonsFor(Appointment appointment) {
        return List.of(
                new ReplyButton(confirmButtonId(appointment.id()), "Confirmar"),
                new ReplyButton(cancelButtonId(appointment.id()), "Cancelar")
        );
    }

    public static boolean isConfirm(String buttonIdOrTitle) {
        if (buttonIdOrTitle == null) {
            return false;
        }
        String value = buttonIdOrTitle.trim();
        return value.startsWith(CONFIRM_PREFIX)
                || value.equalsIgnoreCase("Confirmar")
                || value.toLowerCase(Locale.ROOT).equals("confirm");
    }

    public static boolean isCancel(String buttonIdOrTitle) {
        if (buttonIdOrTitle == null) {
            return false;
        }
        String value = buttonIdOrTitle.trim();
        return value.startsWith(CANCEL_PREFIX)
                || value.equalsIgnoreCase("Cancelar")
                || value.toLowerCase(Locale.ROOT).equals("cancel");
    }

    public static String extractAppointmentKey(String buttonId) {
        if (buttonId == null) {
            return null;
        }
        if (buttonId.startsWith(CONFIRM_PREFIX)) {
            return buttonId.substring(CONFIRM_PREFIX.length());
        }
        if (buttonId.startsWith(CANCEL_PREFIX)) {
            return buttonId.substring(CANCEL_PREFIX.length());
        }
        return null;
    }

    public static ReminderChannel channel() {
        return ReminderChannel.DAY_BEFORE;
    }

    private static String patientName(Appointment appointment) {
        if (appointment.patientName() == null || appointment.patientName().isBlank()) {
            return "paciente";
        }
        return appointment.patientName();
    }
}
