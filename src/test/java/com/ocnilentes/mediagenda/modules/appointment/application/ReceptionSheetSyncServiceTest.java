package com.ocnilentes.mediagenda.modules.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.AppointmentJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.JpaAppointmentRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.SpringDataAppointmentSheetInboxRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReceptionSheetSyncServiceTest {

    private JpaAppointmentRepository repository;
    private SpringDataAppointmentSheetInboxRepository inboxRepository;
    private AppointmentEventService appointmentEventService;
    private ReceptionSheetSyncService service;

    @BeforeEach
    void setUp() {
        repository = mock(JpaAppointmentRepository.class);
        inboxRepository = mock(SpringDataAppointmentSheetInboxRepository.class);
        appointmentEventService = mock(AppointmentEventService.class);
        service = new ReceptionSheetSyncService(
                repository,
                inboxRepository,
                appointmentEventService
        );
    }

    @Test
    void createsAppointmentFromEditedSheetRow() {
        ReceptionSheetSyncService.SheetRowChange change = change(
                "Paciente",
                "1001001001",
                "573000000001"
        );
        when(repository.findBySheetLocation("A_2026_09", 12)).thenReturn(Optional.empty());
        when(repository.upsertFromSheet(
                any(Appointment.class),
                eq("A_2026_09"),
                eq(12),
                eq(AppointmentJpaEntity.Origin.SHEET)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        ReceptionSheetSyncService.SyncResult result = service.apply(change);

        assertEquals("CREATED", result.action());
    }

    @Test
    void cancelsAppointmentWhenReceptionClearsRow() {
        Appointment existing = new Appointment(
                "appointment-1",
                LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0),
                Consultorio.A,
                "Paciente",
                "1001001001",
                "573000000001",
                null,
                12
        );
        when(repository.findBySheetLocation("A_2026_09", 12)).thenReturn(Optional.of(existing));

        ReceptionSheetSyncService.SyncResult result = service.apply(change("", "", ""));

        assertEquals("CANCELLED", result.action());
        verify(repository).delete(existing);
    }

    @Test
    void ignoresAlreadyProcessedEvent() {
        when(inboxRepository.existsById("event-1")).thenReturn(true);

        ReceptionSheetSyncService.SyncResult result =
                service.apply(change("Paciente", "1001001001", "573000000001"));

        assertEquals("DUPLICATE", result.action());
        verify(repository, org.mockito.Mockito.never()).upsertFromSheet(any(), any(), anyInt(), any());
    }

    private ReceptionSheetSyncService.SheetRowChange change(
            String name,
            String cedula,
            String phone
    ) {
        return new ReceptionSheetSyncService.SheetRowChange(
                "event-1",
                null,
                "A_2026_09",
                12,
                LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0),
                Consultorio.A,
                name,
                cedula,
                phone,
                ""
        );
    }
}
