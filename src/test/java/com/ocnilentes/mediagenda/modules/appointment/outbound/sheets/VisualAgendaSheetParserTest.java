package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisualAgendaSheetParserTest {

    @Test
    void shouldBuildSheetNameAndSyntheticId() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        assertEquals("A_2026_08", VisualAgendaSheetParser.sheetName(Consultorio.A, date));
        assertEquals(
                "B|2026-08-05|10:20",
                VisualAgendaSheetParser.syntheticId(Consultorio.B, date, LocalTime.of(10, 20))
        );
    }

    @Test
    void shouldFindFreeAndOccupiedSlots() {
        List<List<Object>> rows = List.of(
                List.of("DIA", "2026-08-05", "MIERCOLES"),
                List.of("HORA", "NOMBRE", "CEDULA", "TELEFONO", "OBSERVACION", "ASISTENCIA"),
                List.of("08:00", "", "", "", "", ""),
                List.of("08:20", "Julian", "1234567890", "573043370408", "", "CONFIRMO"),
                List.of("12:00", "ALMUERZO", "", "", "", "")
        );

        assertTrue(VisualAgendaSheetParser.findSlot(rows, LocalDate.of(2026, 8, 5), LocalTime.of(8, 0)).isPresent());
        assertTrue(VisualAgendaSheetParser.findSlot(rows, LocalDate.of(2026, 8, 5), LocalTime.of(8, 20)).isPresent());

        List<Appointment> appointments = VisualAgendaSheetParser.findAppointmentsInSheet(rows, Consultorio.A);
        assertEquals(1, appointments.size());
        assertEquals("573043370408", appointments.getFirst().phone());
        assertEquals("1234567890", appointments.getFirst().cedula());
        assertEquals("CONFIRMO", appointments.getFirst().asistencia());
        assertFalse(VisualAgendaSheetParser.isOccupied("ALMUERZO", ""));
    }
}
