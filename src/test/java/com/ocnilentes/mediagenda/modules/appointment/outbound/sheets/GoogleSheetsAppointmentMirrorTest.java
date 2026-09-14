package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsApiClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GoogleSheetsAppointmentMirrorTest {

    private static final String SPREADSHEET_A = "spreadsheet-a";

    @Test
    void doesNotDeleteRowOwnedByAnotherAppointment() {
        GoogleSheetsApiClient client = mock(GoogleSheetsApiClient.class);
        GoogleSheetsProperties properties = properties();
        when(client.getValues(eq(SPREADSHEET_A), anyString()))
                .thenReturn(rows("other-appointment", "Otra", "200", "5732", "nota"));
        GoogleSheetsAppointmentMirror mirror = new GoogleSheetsAppointmentMirror(client, properties);

        assertThrows(AppointmentException.class, () -> mirror.delete(appointment()));

        verify(client, never()).updateValues(anyString(), anyString(), any());
    }

    @Test
    void preservesObservationAndWritesStableId() {
        GoogleSheetsApiClient client = mock(GoogleSheetsApiClient.class);
        GoogleSheetsProperties properties = properties();
        Appointment appointment = appointment();
        when(client.getValues(eq(SPREADSHEET_A), anyString())).thenReturn(
                rows(appointment.id(), "Paciente", appointment.cedula(), appointment.phone(), "Conservar")
        );
        GoogleSheetsAppointmentMirror mirror = new GoogleSheetsAppointmentMirror(client, properties);
        ArgumentCaptor<List<List<Object>>> values = ArgumentCaptor.forClass(List.class);

        mirror.upsert(appointment);

        verify(client).updateValues(eq(SPREADSHEET_A), eq("A_2026_09!B3:G3"), values.capture());
        assertEquals("Conservar", values.getValue().getFirst().get(3));
        assertEquals(appointment.id(), values.getValue().getFirst().get(5));
    }

    private GoogleSheetsProperties properties() {
        return new GoogleSheetsProperties(
                true,
                null,
                SPREADSHEET_A,
                "spreadsheet-b",
                "spreadsheet-shared",
                null,
                null,
                null,
                "Festivos",
                "Informacion"
        );
    }

    private Appointment appointment() {
        return new Appointment(
                "11111111-1111-1111-1111-111111111111",
                LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0),
                Consultorio.A,
                "Paciente",
                "1001001001",
                "573000000001",
                null,
                3
        );
    }

    private List<List<Object>> rows(
            String id,
            String name,
            String cedula,
            String phone,
            String observation
    ) {
        return List.of(
                List.of("DIA", "2026-09-10", "JUEVES"),
                List.of("HORA", "NOMBRE", "CEDULA", "TELEFONO", "OBSERVACION", "ASISTENCIA", "APPOINTMENT_ID"),
                List.of("09:00", name, cedula, phone, observation, "", id)
        );
    }
}
