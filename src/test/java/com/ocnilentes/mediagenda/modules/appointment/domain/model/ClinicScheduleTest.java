package com.ocnilentes.mediagenda.modules.appointment.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
