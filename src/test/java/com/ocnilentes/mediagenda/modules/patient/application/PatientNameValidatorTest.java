package com.ocnilentes.mediagenda.modules.patient.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import org.junit.jupiter.api.Test;

class PatientNameValidatorTest {

    @Test
    void shouldAcceptSimpleNameAndCollapseSpaces() {
        assertEquals("María Pérez", PatientNameValidator.normalizeAndValidate("  María   Pérez  "));
        assertEquals("Nazly Ortíz", PatientNameValidator.normalizeAndValidate("Nazly Ortíz"));
    }

    @Test
    void shouldRejectEmptyTooLongAndSpecialChars() {
        PatientException empty = assertThrows(
                PatientException.class,
                () -> PatientNameValidator.normalizeAndValidate("   ")
        );
        assertEquals(PatientNameValidator.EMPTY_MESSAGE, empty.getMessage());

        assertThrows(PatientException.class, () -> PatientNameValidator.normalizeAndValidate(""));

        String tooLong = "Viviana Alejandra Garcia Ramirez Extra";
        PatientException length = assertThrows(
                PatientException.class,
                () -> PatientNameValidator.normalizeAndValidate(tooLong)
        );
        assertEquals(PatientNameValidator.TOO_LONG_MESSAGE, length.getMessage());

        PatientException special = assertThrows(
                PatientException.class,
                () -> PatientNameValidator.normalizeAndValidate(
                        "Rubby Liliana (referida Angelli)"
                )
        );
        assertEquals(PatientNameValidator.SPECIAL_CHARS_MESSAGE, special.getMessage());
    }

    @Test
    void shouldUseAtMostTwoWordsInReminders() {
        assertEquals(
                "Rubby Liliana",
                PatientNameValidator.forReminder(
                        "Rubby Liliana Velasco Mayor (referida Angelli Gómez siempre voy con Leo y/o Sr Octavio)"
                )
        );
        assertEquals("Nazly Ortíz", PatientNameValidator.forReminder("Nazly Ortíz"));
        assertEquals("paciente", PatientNameValidator.forReminder("   "));
        assertEquals("paciente", PatientNameValidator.forReminder(null));
    }
}
