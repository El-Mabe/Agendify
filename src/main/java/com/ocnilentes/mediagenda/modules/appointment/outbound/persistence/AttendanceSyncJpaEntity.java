package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_sync")
public class AttendanceSyncJpaEntity {

    @Id
    @Column(name = "appointment_key", length = 64)
    private String appointmentKey;

    @Column(name = "attended_date", nullable = false)
    private LocalDate attendedDate;

    @Column(name = "cedula", length = 20)
    private String cedula;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected AttendanceSyncJpaEntity() {
    }

    public AttendanceSyncJpaEntity(String appointmentKey, LocalDate attendedDate, String cedula) {
        this.appointmentKey = appointmentKey;
        this.attendedDate = attendedDate;
        this.cedula = cedula;
    }

    @PrePersist
    void onCreate() {
        syncedAt = Instant.now();
    }

    String getAppointmentKey() {
        return appointmentKey;
    }
}
