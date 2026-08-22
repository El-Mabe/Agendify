package com.ocnilentes.mediagenda.modules.appointment.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentEvent(
        Long id,
        String appointmentKey,
        AppointmentEventType eventType,
        String cedula,
        String patientName,
        String phone,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        String consultorio,
        Instant createdAt
) {
}
