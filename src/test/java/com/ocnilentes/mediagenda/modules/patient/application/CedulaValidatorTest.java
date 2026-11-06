package com.ocnilentes.mediagenda.modules.patient.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import org.junit.jupiter.api.Test;

class CedulaValidatorTest {

    @Test
    void shouldAcceptDigitsOnlyAndStoreWithoutSeparators() {
        assertEquals("1234567890", CedulaValidator.normalizeAndValidate("1234567890"));
        assertEquals("1234567", CedulaValidator.normalizeAndValidate("1234567"));
        assertEquals("1234567", CedulaValidator.normalizeAndValidate(" 1234567 "));
    }

    @Test
    void shouldRejectDotsAndCommasWithoutNormalizingThemAway() {
        PatientException dots = assertThrows(
                PatientException.class,
                () -> CedulaValidator.normalizeAndValidate("1.234.567.890")
        );
        assertEquals(CedulaValidator.PUNCTUATION_MESSAGE, dots.getMessage());

        PatientException commas = assertThrows(
                PatientException.class,
                () -> CedulaValidator.normalizeAndValidate("1,234,567,890")
        );
        assertEquals(CedulaValidator.PUNCTUATION_MESSAGE, commas.getMessage());
    }

    @Test
    void shouldRejectInvalidCedula() {
        assertThrows(PatientException.class, () -> CedulaValidator.normalizeAndValidate("abc"));
        assertThrows(PatientException.class, () -> CedulaValidator.normalizeAndValidate("12345"));
        assertThrows(PatientException.class, () -> CedulaValidator.normalizeAndValidate(""));
    }
}
