package com.ocnilentes.mediagenda.modules.reminder.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentReminder(
        Long id,
        String appointmentKey,
        ReminderChannel channel,
        String phone,
        String patientName,
        String cedula,
        LocalDate appointmentDate,
        LocalTime appointmentTime,
        ReminderStatus status,
        String metaMessageId,
        String errorMessage,
        Instant sentAt,
        Instant respondedAt
) {
}
