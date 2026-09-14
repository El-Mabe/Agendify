package com.ocnilentes.mediagenda.modules.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.AppointmentSheetSyncJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.JpaAppointmentRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.SpringDataAppointmentSheetSyncRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.sheets.GoogleSheetsAppointmentMirror;
import com.ocnilentes.mediagenda.shared.config.AppointmentSheetSyncProperties;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AppointmentSheetSyncServiceTest {

    private SpringDataAppointmentSheetSyncRepository syncRepository;
    private JpaAppointmentRepository appointmentRepository;
    private GoogleSheetsAppointmentMirror mirror;
    private AppointmentSheetSyncService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        syncRepository = mock(SpringDataAppointmentSheetSyncRepository.class);
        appointmentRepository = mock(JpaAppointmentRepository.class);
        mirror = mock(GoogleSheetsAppointmentMirror.class);
        ObjectProvider<GoogleSheetsAppointmentMirror> mirrorProvider = mock(ObjectProvider.class);
        when(mirrorProvider.getIfAvailable()).thenReturn(mirror);
        when(syncRepository.findByAppointmentIdAndAppointmentVersionAndOperation(
                any(),
                anyLong(),
                any()
        )).thenReturn(Optional.empty());
        when(appointmentRepository.findVersion(any())).thenReturn(0L);
        when(syncRepository.claim(anyLong(), any(), any())).thenReturn(1);
        when(syncRepository.save(any())).thenAnswer(invocation -> {
            AppointmentSheetSyncJpaEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                ReflectionTestUtils.setField(entity, "id", 1L);
            }
            return entity;
        });
        TaskExecutor directExecutor = Runnable::run;
        AppointmentSheetSyncProperties properties = new AppointmentSheetSyncProperties(
                true,
                "token",
                true,
                1,
                3,
                true,
                1
        );
        service = new AppointmentSheetSyncService(
                syncRepository,
                appointmentRepository,
                mirrorProvider,
                directExecutor,
                properties
        );
    }

    @Test
    void keepsFailedSheetWriteForRetry() {
        Appointment appointment = appointment();
        AppointmentSheetSyncJpaEntity[] stored = new AppointmentSheetSyncJpaEntity[1];
        when(syncRepository.save(any())).thenAnswer(invocation -> {
            AppointmentSheetSyncJpaEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 1L);
            stored[0] = entity;
            return entity;
        });
        when(syncRepository.findById(1L)).thenAnswer(invocation -> Optional.of(stored[0]));
        when(appointmentRepository.findAnyById(appointment.id())).thenReturn(Optional.of(appointment));
        when(mirror.upsert(appointment)).thenThrow(new RuntimeException("Sheets unavailable"));

        service.syncCreated(appointment);

        assertEquals(AppointmentSheetSyncJpaEntity.Status.FAILED, stored[0].getStatus());
        assertEquals(1, stored[0].getAttempts());
        assertNotNull(stored[0].getNextAttemptAt());
    }

    @Test
    void retriesPendingWriteAndMarksItSynced() {
        Appointment appointment = appointment();
        AppointmentSheetSyncJpaEntity sync = new AppointmentSheetSyncJpaEntity(
                appointment.id(),
                0L,
                AppointmentSheetSyncJpaEntity.Operation.UPSERT
        );
        ReflectionTestUtils.setField(sync, "id", 2L);
        sync.setStatus(AppointmentSheetSyncJpaEntity.Status.FAILED);
        when(syncRepository.findTop50ByStatusInAndNextAttemptAtBeforeOrderByUpdatedAtAsc(
                any(),
                any()
        )).thenReturn(List.of(sync));
        when(appointmentRepository.findAnyById(appointment.id())).thenReturn(Optional.of(appointment));
        when(mirror.upsert(appointment)).thenReturn(
                new GoogleSheetsAppointmentMirror.SheetLocation("spreadsheet-a", "A_2026_09", 12)
        );

        service.retryPending();

        assertEquals(AppointmentSheetSyncJpaEntity.Status.SYNCED, sync.getStatus());
        verify(appointmentRepository).updateSheetLocation(appointment.id(), "A_2026_09", 12);
    }

    @Test
    void keepsDifferentAppointmentVersionsAsSeparateOutboxEvents() {
        Appointment appointment = appointment();
        when(appointmentRepository.findVersion(appointment.id())).thenReturn(0L, 1L);

        service.syncCreated(appointment);
        service.syncCancelled(appointment);

        ArgumentCaptor<AppointmentSheetSyncJpaEntity> captor =
                ArgumentCaptor.forClass(AppointmentSheetSyncJpaEntity.class);
        verify(syncRepository, times(2)).save(captor.capture());
        assertEquals(AppointmentSheetSyncJpaEntity.Operation.UPSERT, captor.getAllValues().get(0).getOperation());
        assertEquals(AppointmentSheetSyncJpaEntity.Operation.DELETE, captor.getAllValues().get(1).getOperation());
        assertEquals(0L, captor.getAllValues().get(0).getAppointmentVersion());
        assertEquals(1L, captor.getAllValues().get(1).getAppointmentVersion());
    }

    private Appointment appointment() {
        return new Appointment(
                "appointment-1",
                LocalDate.of(2026, 9, 10),
                LocalTime.of(9, 0),
                Consultorio.A,
                "Paciente",
                "1001001001",
                "573000000001",
                null,
                null
        );
    }
}
