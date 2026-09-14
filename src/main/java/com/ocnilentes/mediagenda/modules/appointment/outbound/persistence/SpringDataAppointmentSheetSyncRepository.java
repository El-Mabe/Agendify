package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataAppointmentSheetSyncRepository
        extends JpaRepository<AppointmentSheetSyncJpaEntity, Long> {

    List<AppointmentSheetSyncJpaEntity> findTop50ByStatusInAndNextAttemptAtBeforeOrderByUpdatedAtAsc(
            List<AppointmentSheetSyncJpaEntity.Status> statuses,
            Instant nextAttemptAt
    );

    boolean existsByAppointmentIdAndIdLessThanAndStatusNot(
            String appointmentId,
            Long id,
            AppointmentSheetSyncJpaEntity.Status status
    );

    Optional<AppointmentSheetSyncJpaEntity> findByAppointmentIdAndAppointmentVersionAndOperation(
            String appointmentId,
            long appointmentVersion,
            AppointmentSheetSyncJpaEntity.Operation operation
    );

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE appointment_sheet_sync
               SET status = 'PROCESSING',
                   locked_at = :now,
                   next_attempt_at = :leaseUntil,
                   updated_at = :now
             WHERE id = :id
               AND (
                    status IN ('PENDING', 'FAILED')
                    OR (status = 'PROCESSING' AND next_attempt_at < :now)
               )
            """, nativeQuery = true)
    int claim(
            @Param("id") Long id,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil
    );
}
