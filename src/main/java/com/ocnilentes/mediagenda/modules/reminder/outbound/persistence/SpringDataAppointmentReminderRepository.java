package com.ocnilentes.mediagenda.modules.reminder.outbound.persistence;

import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAppointmentReminderRepository extends JpaRepository<AppointmentReminderJpaEntity, Long> {

    Optional<AppointmentReminderJpaEntity> findByAppointmentKeyAndChannel(
            String appointmentKey,
            ReminderChannel channel
    );

    List<AppointmentReminderJpaEntity> findByPhoneAndStatusIn(String phone, Collection<ReminderStatus> statuses);
}
