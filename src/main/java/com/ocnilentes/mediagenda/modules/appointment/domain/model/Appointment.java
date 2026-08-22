package com.ocnilentes.mediagenda.modules.appointment.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Appointment(
        String id,
        LocalDate date,
        LocalTime time,
        Consultorio consultorio,
        String patientName,
        String cedula,
        String phone,
        String asistencia,
        Integer sheetRowNumber
) {
    public Appointment withAsistencia(String value) {
        return new Appointment(
                id, date, time, consultorio, patientName, cedula, phone, value, sheetRowNumber
        );
    }
}
