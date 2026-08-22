package com.ocnilentes.mediagenda.modules.appointment.domain.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Valores permitidos en la columna ASISTENCIA del spreadsheet (desplegable).
 */
public enum AttendanceStatus {
    CONFIRMO,
    CANCELO,
    ASISTIO;

    public static Optional<AttendanceStatus> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace("Ó", "O")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ú", "U");
        try {
            return Optional.of(AttendanceStatus.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
