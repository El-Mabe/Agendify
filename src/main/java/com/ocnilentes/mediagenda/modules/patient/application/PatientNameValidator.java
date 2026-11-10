package com.ocnilentes.mediagenda.modules.patient.application;

import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public final class PatientNameValidator {

    static final int MAX_CHAT_LENGTH = 40;
    static final String EMPTY_MESSAGE = "El nombre no puede ir vacío. Escribe tu nombre y apellido.";
    static final String TOO_LONG_MESSAGE =
            "El nombre es demasiado largo. Escríbelo en máximo 40 caracteres, solo nombre y apellido.";
    static final String SPECIAL_CHARS_MESSAGE =
            "Escribe solo tu nombre y apellido, con letras y espacios, sin paréntesis ni notas.";

    private static final Pattern LETTERS_AND_SPACES = Pattern.compile("^[\\p{L}]+(?:[ ]+[\\p{L}]+)*$");

    private PatientNameValidator() {
    }

    public static String normalizeAndValidate(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new PatientException(EMPTY_MESSAGE);
        }
        String normalized = raw.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            throw new PatientException(EMPTY_MESSAGE);
        }
        if (normalized.length() > MAX_CHAT_LENGTH) {
            throw new PatientException(TOO_LONG_MESSAGE);
        }
        if (!LETTERS_AND_SPACES.matcher(normalized).matches()) {
            throw new PatientException(SPECIAL_CHARS_MESSAGE);
        }
        return normalized;
    }

    public static String forReminder(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "paciente";
        }
        String cleaned = raw.trim()
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!StringUtils.hasText(cleaned)) {
            return "paciente";
        }
        String twoWords = Arrays.stream(cleaned.split(" "))
                .filter(StringUtils::hasText)
                .limit(2)
                .collect(Collectors.joining(" "));
        return StringUtils.hasText(twoWords) ? twoWords : "paciente";
    }
}
