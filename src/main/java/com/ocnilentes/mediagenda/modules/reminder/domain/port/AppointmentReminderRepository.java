package com.ocnilentes.mediagenda.modules.reminder.domain.port;

import com.ocnilentes.mediagenda.modules.reminder.domain.model.AppointmentReminder;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import java.util.List;
import java.util.Optional;

public interface AppointmentReminderRepository {

    Optional<AppointmentReminder> findByAppointmentKeyAndChannel(String appointmentKey, ReminderChannel channel);

    List<AppointmentReminder> findByPhoneAndStatusIn(String phone, List<ReminderStatus> statuses);

    AppointmentReminder save(AppointmentReminder reminder);
}
