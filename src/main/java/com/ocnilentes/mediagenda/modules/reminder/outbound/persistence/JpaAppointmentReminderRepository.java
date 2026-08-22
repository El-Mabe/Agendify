package com.ocnilentes.mediagenda.modules.reminder.outbound.persistence;

import com.ocnilentes.mediagenda.modules.reminder.domain.model.AppointmentReminder;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import com.ocnilentes.mediagenda.modules.reminder.domain.port.AppointmentReminderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaAppointmentReminderRepository implements AppointmentReminderRepository {

    private final SpringDataAppointmentReminderRepository springData;

    JpaAppointmentReminderRepository(SpringDataAppointmentReminderRepository springData) {
        this.springData = springData;
    }

    @Override
    public Optional<AppointmentReminder> findByAppointmentKeyAndChannel(
            String appointmentKey,
            ReminderChannel channel
    ) {
        return springData.findByAppointmentKeyAndChannel(appointmentKey, channel).map(this::toDomain);
    }

    @Override
    public List<AppointmentReminder> findByPhoneAndStatusIn(String phone, List<ReminderStatus> statuses) {
        return springData.findByPhoneAndStatusIn(phone, statuses).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public AppointmentReminder save(AppointmentReminder reminder) {
        AppointmentReminderJpaEntity entity = reminder.id() == null
                ? new AppointmentReminderJpaEntity()
                : springData.findById(reminder.id()).orElseGet(AppointmentReminderJpaEntity::new);

        entity.setAppointmentKey(reminder.appointmentKey());
        entity.setChannel(reminder.channel());
        entity.setPhone(reminder.phone());
        entity.setPatientName(reminder.patientName());
        entity.setCedula(reminder.cedula());
        entity.setAppointmentDate(reminder.appointmentDate());
        entity.setAppointmentTime(reminder.appointmentTime());
        entity.setStatus(reminder.status());
        entity.setMetaMessageId(reminder.metaMessageId());
        entity.setErrorMessage(reminder.errorMessage());
        entity.setSentAt(reminder.sentAt());
        entity.setRespondedAt(reminder.respondedAt());

        return toDomain(springData.save(entity));
    }

    private AppointmentReminder toDomain(AppointmentReminderJpaEntity entity) {
        return new AppointmentReminder(
                entity.getId(),
                entity.getAppointmentKey(),
                entity.getChannel(),
                entity.getPhone(),
                entity.getPatientName(),
                entity.getCedula(),
                entity.getAppointmentDate(),
                entity.getAppointmentTime(),
                entity.getStatus(),
                entity.getMetaMessageId(),
                entity.getErrorMessage(),
                entity.getSentAt(),
                entity.getRespondedAt()
        );
    }
}
