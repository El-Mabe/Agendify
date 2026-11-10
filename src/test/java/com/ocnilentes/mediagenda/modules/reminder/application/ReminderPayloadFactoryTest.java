package com.ocnilentes.mediagenda.modules.reminder.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReminderPayloadFactoryTest {

    @Test
    void shouldFormatDateInSpanishWithoutNumericDayMonth() {
        assertEquals("sábado 7 de noviembre", ReminderPayloadFactory.formatDate(LocalDate.of(2026, 11, 7)));
    }

    @Test
    void shouldFormatTimeIn12HourSpanish() {
        assertEquals("9:00 a. m.", ReminderPayloadFactory.formatTime(LocalTime.of(9, 0)));
        assertEquals("1:30 p. m.", ReminderPayloadFactory.formatTime(LocalTime.of(13, 30)));
        assertEquals("12:00 p. m.", ReminderPayloadFactory.formatTime(LocalTime.of(12, 0)));
        assertEquals("12:00 a. m.", ReminderPayloadFactory.formatTime(LocalTime.of(0, 0)));
    }

    @Test
    void shouldBuildReminderBodyAndTemplateParameters() {
        Appointment appointment = new Appointment(
                "id",
                LocalDate.of(2026, 11, 7),
                LocalTime.of(9, 0),
                Consultorio.A,
                "María Pérez",
                "1234567890",
                "573000000001",
                null,
                null
        );

        String body = ReminderPayloadFactory.buildBody(appointment);
        assertTrue(body.contains("Hola, María Pérez 😊"));
        assertTrue(body.contains("📅 Fecha: sábado 7 de noviembre"));
        assertTrue(body.contains("🕘 Hora: 9:00 a. m."));
        assertTrue(body.contains("📍 Lugar: " + ReminderPayloadFactory.CLINIC_PLACE));

        assertEquals(
                List.of("María Pérez", "sábado 7 de noviembre", "9:00 a. m."),
                ReminderPayloadFactory.templateBodyParameters(appointment)
        );
    }

    @Test
    void shouldUseAtMostTwoWordsAsTemplateName() {
        Appointment appointment = new Appointment(
                "id",
                LocalDate.of(2026, 11, 7),
                LocalTime.of(9, 0),
                Consultorio.A,
                "Rubby Liliana Velasco Mayor (referida Angelli Gómez)",
                "1234567890",
                "573000000001",
                null,
                null
        );
        assertEquals(
                List.of("Rubby Liliana", "sábado 7 de noviembre", "9:00 a. m."),
                ReminderPayloadFactory.templateBodyParameters(appointment)
        );
        assertTrue(ReminderPayloadFactory.buildBody(appointment).contains("Hola, Rubby Liliana 😊"));
    }
}
