package com.ocnilentes.mediagenda.modules.appointment.domain.port;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEvent;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import java.util.Optional;

public interface AppointmentEventRepository {

    AppointmentEvent save(AppointmentEvent event);

    boolean existsByAppointmentKeyAndType(String appointmentKey, AppointmentEventType eventType);

    Optional<AppointmentEvent> findLatestCancelledByAppointmentKey(String appointmentKey);
}
