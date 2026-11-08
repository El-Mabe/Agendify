package com.ocnilentes.mediagenda.modules.appointment.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttendanceStatusTest {

    @Test
    void shouldParseAccentInsensitiveValues() {
        assertEquals(AttendanceStatus.ASISTIO, AttendanceStatus.parse("Asistió").orElseThrow());
        assertEquals(AttendanceStatus.CONFIRMO, AttendanceStatus.parse("confirmo").orElseThrow());
        assertEquals(AttendanceStatus.CANCELO, AttendanceStatus.parse("CANCELO").orElseThrow());
        assertEquals(AttendanceStatus.NO_ASISTIO, AttendanceStatus.parse("NO ASISTIO").orElseThrow());
        assertTrue(AttendanceStatus.parse("").isEmpty());
        assertTrue(AttendanceStatus.parse("OTRO").isEmpty());
    }
}
