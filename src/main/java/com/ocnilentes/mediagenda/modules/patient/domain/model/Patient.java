package com.ocnilentes.mediagenda.modules.patient.domain.model;

import java.time.LocalDate;

public record Patient(
        String cedula,
        String fullName,
        String lastPhone,
        LocalDate lastAttendedAt,
        LocalDate nextFollowUpOn,
        FollowUpStatus followUpStatus
) {
    public Patient {
        if (followUpStatus == null) {
            followUpStatus = FollowUpStatus.NONE;
        }
    }

    public static Patient basic(String cedula, String fullName, String lastPhone) {
        return new Patient(cedula, fullName, lastPhone, null, null, FollowUpStatus.NONE);
    }
}
