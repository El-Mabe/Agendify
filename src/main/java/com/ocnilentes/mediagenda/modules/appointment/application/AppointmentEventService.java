package com.ocnilentes.mediagenda.modules.appointment.application;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEvent;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import com.ocnilentes.mediagenda.modules.appointment.domain.port.AppointmentEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AppointmentEventService {

    private final AppointmentEventRepository appointmentEventRepository;

    public AppointmentEventService(AppointmentEventRepository appointmentEventRepository) {
        this.appointmentEventRepository = appointmentEventRepository;
    }

    public void record(Appointment appointment, AppointmentEventType type, String actorPhone) {
        if (appointment == null || type == null) {
            return;
        }
        String phone = StringUtils.hasText(actorPhone) ? actorPhone.trim() : appointment.phone();
        if (!StringUtils.hasText(phone)) {
            phone = "unknown";
        }
        appointmentEventRepository.save(new AppointmentEvent(
                null,
                appointment.id(),
                type,
                appointment.cedula(),
                appointment.patientName(),
                phone,
                appointment.date(),
                appointment.time(),
                appointment.consultorio() == null ? null : appointment.consultorio().name(),
                null
        ));
    }

    public boolean alreadyAttended(String appointmentKey) {
        return StringUtils.hasText(appointmentKey)
                && appointmentEventRepository.existsByAppointmentKeyAndType(
                appointmentKey,
                AppointmentEventType.ATTENDED
        );
    }
}
