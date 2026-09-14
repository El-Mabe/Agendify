package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentJpaEntity, String> {

    List<AppointmentJpaEntity> findByPhoneAndStatusOrderByDateAscTimeAsc(
            String phone,
            AppointmentJpaEntity.Status status
    );

    List<AppointmentJpaEntity> findByCedulaAndStatusOrderByDateAscTimeAsc(
            String cedula,
            AppointmentJpaEntity.Status status
    );

    Optional<AppointmentJpaEntity> findFirstByCedulaAndDateAndStatus(
            String cedula,
            LocalDate date,
            AppointmentJpaEntity.Status status
    );

    List<AppointmentJpaEntity> findByDateAndStatusOrderByTimeAsc(
            LocalDate date,
            AppointmentJpaEntity.Status status
    );

    List<AppointmentJpaEntity> findByAsistenciaAndStatusOrderByDateAscTimeAsc(
            String asistencia,
            AppointmentJpaEntity.Status status
    );

    Optional<AppointmentJpaEntity> findByIdAndStatus(
            String id,
            AppointmentJpaEntity.Status status
    );

    Optional<AppointmentJpaEntity> findByLegacyKeyAndStatus(
            String legacyKey,
            AppointmentJpaEntity.Status status
    );

    boolean existsByDateAndTimeAndConsultorioAndStatus(
            LocalDate date,
            LocalTime time,
            Consultorio consultorio,
            AppointmentJpaEntity.Status status
    );

    long countByDateAndConsultorioAndStatus(
            LocalDate date,
            Consultorio consultorio,
            AppointmentJpaEntity.Status status
    );

    Optional<AppointmentJpaEntity> findBySheetNameAndSheetRowNumberAndStatus(
            String sheetName,
            Integer sheetRowNumber,
            AppointmentJpaEntity.Status status
    );

    List<AppointmentJpaEntity> findBySheetNameAndStatus(
            String sheetName,
            AppointmentJpaEntity.Status status
    );
}
