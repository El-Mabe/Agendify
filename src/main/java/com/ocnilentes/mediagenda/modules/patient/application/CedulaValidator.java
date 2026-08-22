package com.ocnilentes.mediagenda.modules.patient.application;

import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class CedulaValidator {

    private static final Pattern DIGITS = Pattern.compile("^\\d{6,10}$");

    private CedulaValidator() {
    }

    public static String normalizeAndValidate(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new PatientException("Por favor escribe tu número de cédula (solo números).");
        }

        String digits = raw.trim().replaceAll("[.\\s-]", "");
        if (!DIGITS.matcher(digits).matches()) {
            throw new PatientException(
                    "Cédula inválida. Escribe solo números, entre 6 y 10 dígitos (sin puntos ni espacios)."
            );
        }
        return digits;
    }
}
