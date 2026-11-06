package com.ocnilentes.mediagenda.modules.patient.application;

import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class CedulaValidator {

    private static final Pattern DIGITS = Pattern.compile("^\\d{6,10}$");
    static final String PUNCTUATION_MESSAGE =
            "Escribe tu cédula solo con números, sin puntos ni comas. Vuelve a intentarlo.";
    static final String INVALID_MESSAGE =
            "Cédula inválida. Escribe solo números, entre 6 y 10 dígitos.";

    private CedulaValidator() {
    }

    public static String normalizeAndValidate(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new PatientException("Por favor escribe tu número de cédula (solo números).");
        }

        String trimmed = raw.trim();
        if (trimmed.indexOf('.') >= 0 || trimmed.indexOf(',') >= 0) {
            throw new PatientException(PUNCTUATION_MESSAGE);
        }

        String digits = trimmed.replaceAll("[\\s-]", "");
        if (!DIGITS.matcher(digits).matches()) {
            throw new PatientException(INVALID_MESSAGE);
        }
        return digits;
    }
}
