package com.ocnilentes.mediagenda.modules.appointment.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ClinicScheduleTest {

    @Test
    void shouldCoverCrossLunchHours() {
        assertTrue(ClinicSchedule.offers(Consultorio.A, LocalTime.of(8, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, LocalTime.of(8, 0)));

        assertTrue(ClinicSchedule.offers(Consultorio.B, LocalTime.of(12, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.A, LocalTime.of(12, 0)));

        assertTrue(ClinicSchedule.offers(Consultorio.A, LocalTime.of(13, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, LocalTime.of(13, 0)));

        assertTrue(ClinicSchedule.offers(Consultorio.B, LocalTime.of(17, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.A, LocalTime.of(17, 0)));
    }

    @Test
    void saturdayBlocksNoonLunchInBothConsultorios() {
        LocalDate saturday = LocalDate.of(2026, 11, 14);
        assertFalse(ClinicSchedule.offers(Consultorio.A, saturday, LocalTime.of(12, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(12, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(12, 20)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(12, 40)));
        assertTrue(ClinicSchedule.offers(Consultorio.A, saturday, LocalTime.of(8, 0)));
        assertTrue(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(8, 0)));
        assertTrue(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(11, 40)));
        assertTrue(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(13, 0)));
        assertTrue(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(16, 40)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(17, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, saturday, LocalTime.of(17, 40)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, LocalDate.of(2026, 11, 13), LocalTime.of(8, 0)));
        assertFalse(ClinicSchedule.offers(Consultorio.B, LocalDate.of(2026, 11, 13), LocalTime.of(13, 0)));
        assertTrue(ClinicSchedule.offers(Consultorio.B, LocalDate.of(2026, 11, 13), LocalTime.of(12, 0)));
        assertTrue(ClinicSchedule.offers(Consultorio.B, LocalDate.of(2026, 11, 13), LocalTime.of(17, 0)));
    }
}
