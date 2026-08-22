package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEvent;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import com.ocnilentes.mediagenda.modules.appointment.domain.port.AppointmentEventRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaAppointmentEventRepository implements AppointmentEventRepository {

    private final SpringDataAppointmentEventRepository springData;

    JpaAppointmentEventRepository(SpringDataAppointmentEventRepository springData) {
        this.springData = springData;
    }

    @Override
    public AppointmentEvent save(AppointmentEvent event) {
        AppointmentEventJpaEntity entity = new AppointmentEventJpaEntity();
        entity.setAppointmentKey(event.appointmentKey());
        entity.setEventType(event.eventType());
        entity.setCedula(event.cedula());
        entity.setPatientName(event.patientName());
        entity.setPhone(event.phone());
        entity.setAppointmentDate(event.appointmentDate());
        entity.setAppointmentTime(event.appointmentTime());
        entity.setConsultorio(event.consultorio());
        return toDomain(springData.save(entity));
    }

    @Override
    public boolean existsByAppointmentKeyAndType(String appointmentKey, AppointmentEventType eventType) {
        return springData.existsByAppointmentKeyAndEventType(appointmentKey, eventType);
    }

    @Override
    public Optional<AppointmentEvent> findLatestCancelledByAppointmentKey(String appointmentKey) {
        return springData.findFirstByAppointmentKeyAndEventTypeOrderByCreatedAtDesc(
                        appointmentKey,
                        AppointmentEventType.CANCELLED
                )
                .map(this::toDomain);
    }

    private AppointmentEvent toDomain(AppointmentEventJpaEntity entity) {
        return new AppointmentEvent(
                entity.getId(),
                entity.getAppointmentKey(),
                entity.getEventType(),
                entity.getCedula(),
                entity.getPatientName(),
                entity.getPhone(),
                entity.getAppointmentDate(),
                entity.getAppointmentTime(),
                entity.getConsultorio(),
                entity.getCreatedAt()
        );
    }
}
