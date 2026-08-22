package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAttendanceSyncRepository extends JpaRepository<AttendanceSyncJpaEntity, String> {
}
