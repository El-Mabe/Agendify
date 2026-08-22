package com.ocnilentes.mediagenda.modules.reminder.application;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ReplyButton;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ReminderPayloadFactory {

    public static final String CONFIRM_PREFIX = "rc|";
    public static final String CANCEL_PREFIX = "rx|";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private ReminderPayloadFactory() {
    }

    public static String confirmButtonId(String appointmentKey) {
        return CONFIRM_PREFIX + appointmentKey;
    }

    public static String cancelButtonId(String appointmentKey) {
        return CANCEL_PREFIX + appointmentKey;
    }

    public static String buildBody(Appointment appointment) {
        String name = appointment.patientName() == null || appointment.patientName().isBlank()
                ? "paciente"
                : appointment.patientName();
        return """
                ¡Hola, %s!

                Te recordamos tu cita de optometría:
                📅 Fecha: %s
                🕐 Hora: %s

                Por favor confirma o cancela con los botones de abajo.
                """.formatted(
                name,
                appointment.date().format(DATE_FORMAT),
                appointment.time().format(TIME_FORMAT)
        ).trim();
    }

    public static List<String> templateBodyParameters(Appointment appointment) {
        String name = appointment.patientName() == null || appointment.patientName().isBlank()
                ? "paciente"
                : appointment.patientName();
        return List.of(
                name,
                appointment.date().format(DATE_FORMAT),
                appointment.time().format(TIME_FORMAT)
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
}
