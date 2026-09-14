package com.ocnilentes.mediagenda.modules.appointment.application;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.AppointmentSheetSyncJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.JpaAppointmentRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.SpringDataAppointmentSheetSyncRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.sheets.GoogleSheetsAppointmentMirror;
import com.ocnilentes.mediagenda.shared.config.AppointmentSheetSyncProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AppointmentSheetSyncService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSheetSyncService.class);

    private final SpringDataAppointmentSheetSyncRepository syncRepository;
    private final JpaAppointmentRepository appointmentRepository;
    private final ObjectProvider<GoogleSheetsAppointmentMirror> mirrorProvider;
    private final TaskExecutor taskExecutor;
    private final AppointmentSheetSyncProperties properties;

    public AppointmentSheetSyncService(
            SpringDataAppointmentSheetSyncRepository syncRepository,
            JpaAppointmentRepository appointmentRepository,
            ObjectProvider<GoogleSheetsAppointmentMirror> mirrorProvider,
            @Qualifier("appointmentSheetSyncTaskExecutor") TaskExecutor taskExecutor,
            AppointmentSheetSyncProperties properties
    ) {
        this.syncRepository = syncRepository;
        this.appointmentRepository = appointmentRepository;
        this.mirrorProvider = mirrorProvider;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
    }

    public void syncCreated(Appointment appointment) {
        enqueue(appointment.id(), AppointmentSheetSyncJpaEntity.Operation.UPSERT);
    }

    public void syncCancelled(Appointment appointment) {
        enqueue(appointment.id(), AppointmentSheetSyncJpaEntity.Operation.DELETE);
    }

    public void syncAttendance(Appointment appointment) {
        enqueue(appointment.id(), AppointmentSheetSyncJpaEntity.Operation.ATTENDANCE);
    }

    @Scheduled(fixedDelayString = "${appointments.sheet-sync.retry-delay-ms:60000}")
    public void retryPending() {
        if (!properties.enabled()) {
            return;
        }
        List<AppointmentSheetSyncJpaEntity> pending =
                syncRepository.findTop50ByStatusInAndNextAttemptAtBeforeOrderByUpdatedAtAsc(
                        List.of(
                                AppointmentSheetSyncJpaEntity.Status.PENDING,
                                AppointmentSheetSyncJpaEntity.Status.PROCESSING,
                                AppointmentSheetSyncJpaEntity.Status.FAILED
                        ),
                        Instant.now()
                );
        pending.forEach(this::processSafely);
    }

    private void enqueue(String appointmentId, AppointmentSheetSyncJpaEntity.Operation operation) {
        if (!properties.enabled()) {
            return;
        }
        long version = appointmentRepository.findVersion(appointmentId);
        AppointmentSheetSyncJpaEntity sync = syncRepository
                .findByAppointmentIdAndAppointmentVersionAndOperation(
                        appointmentId,
                        version,
                        operation
                )
                .orElseGet(() -> new AppointmentSheetSyncJpaEntity(appointmentId, version, operation));
        if (sync.getStatus() == AppointmentSheetSyncJpaEntity.Status.SYNCED) {
            return;
        }
        sync.setStatus(AppointmentSheetSyncJpaEntity.Status.PENDING);
        sync.setAttempts(0);
        sync.setNextAttemptAt(Instant.now());
        sync.setLastError(null);
        AppointmentSheetSyncJpaEntity saved = syncRepository.save(sync);
        Runnable dispatch = () -> dispatch(saved.getId(), appointmentId, operation);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    private void dispatch(
            Long syncId,
            String appointmentId,
            AppointmentSheetSyncJpaEntity.Operation operation
    ) {
        try {
            taskExecutor.execute(() -> processById(syncId));
        } catch (RuntimeException exception) {
            log.error(
                    "Appointment Sheets synchronization remains queued appointment={} operation={}",
                    appointmentId,
                    operation,
                    exception
            );
        }
    }

    private void processById(Long syncId) {
        syncRepository.findById(syncId).ifPresent(this::processSafely);
    }

    private synchronized void processSafely(AppointmentSheetSyncJpaEntity sync) {
        if (syncRepository.existsByAppointmentIdAndIdLessThanAndStatusNot(
                sync.getAppointmentId(),
                sync.getId(),
                AppointmentSheetSyncJpaEntity.Status.SYNCED
        )) {
            return;
        }
        Instant now = Instant.now();
        if (syncRepository.claim(sync.getId(), now, now.plus(Duration.ofMinutes(5))) == 0) {
            return;
        }
        sync.setStatus(AppointmentSheetSyncJpaEntity.Status.PROCESSING);
        sync.setLockedAt(now);
        sync.setNextAttemptAt(now.plus(Duration.ofMinutes(5)));
        GoogleSheetsAppointmentMirror mirror = mirrorProvider.getIfAvailable();
        if (mirror == null) {
            markFailure(sync, new IllegalStateException("Google Sheets mirror is not configured"));
            return;
        }

        Appointment appointment = appointmentRepository.findAnyById(sync.getAppointmentId())
                .orElse(null);
        if (appointment == null) {
            markFailure(sync, new IllegalStateException("Appointment not found: " + sync.getAppointmentId()));
            return;
        }

        try {
            switch (sync.getOperation()) {
                case UPSERT -> {
                    GoogleSheetsAppointmentMirror.SheetLocation location = mirror.upsert(appointment);
                    appointmentRepository.updateSheetLocation(
                            appointment.id(),
                            location.sheetName(),
                            location.rowNumber()
                    );
                }
                case DELETE -> mirror.delete(appointment);
                case ATTENDANCE -> {
                    GoogleSheetsAppointmentMirror.SheetLocation location = mirror.updateAttendance(appointment);
                    if (appointment.sheetRowNumber() == null) {
                        appointmentRepository.updateSheetLocation(
                                appointment.id(),
                                location.sheetName(),
                                location.rowNumber()
                        );
                    }
                }
            }
            markSynced(sync);
        } catch (RuntimeException exception) {
            markFailure(sync, exception);
        }
    }

    private void markSynced(AppointmentSheetSyncJpaEntity sync) {
        sync.setStatus(AppointmentSheetSyncJpaEntity.Status.SYNCED);
        sync.setLockedAt(null);
        sync.setNextAttemptAt(null);
        sync.setLastError(null);
        syncRepository.save(sync);
    }

    private void markFailure(AppointmentSheetSyncJpaEntity sync, RuntimeException exception) {
        int attempts = sync.getAttempts() + 1;
        long delayMinutes = Math.min(30, 1L << Math.min(attempts - 1, 5));
        String message = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        sync.setAttempts(attempts);
        sync.setStatus(AppointmentSheetSyncJpaEntity.Status.FAILED);
        sync.setLockedAt(null);
        sync.setNextAttemptAt(Instant.now().plus(Duration.ofMinutes(delayMinutes)));
        sync.setLastError(message.substring(0, Math.min(message.length(), 1000)));
        syncRepository.save(sync);
        log.error(
                "Failed to synchronize appointment={} operation={} attempt={}",
                sync.getAppointmentId(),
                sync.getOperation(),
                attempts,
                exception
        );
    }
}
