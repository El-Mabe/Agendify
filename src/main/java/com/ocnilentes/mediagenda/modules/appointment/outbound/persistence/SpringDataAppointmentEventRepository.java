package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAppointmentEventRepository extends JpaRepository<AppointmentEventJpaEntity, Long> {

    boolean existsByAppointmentKeyAndEventType(String appointmentKey, AppointmentEventType eventType);

    Optional<AppointmentEventJpaEntity> findFirstByAppointmentKeyAndEventTypeOrderByCreatedAtDesc(
            String appointmentKey,
            AppointmentEventType eventType
    );
}
