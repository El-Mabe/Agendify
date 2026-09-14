package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "appointment_sheet_inbox")
public class AppointmentSheetInboxJpaEntity {

    @Id
    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "appointment_id", length = 64)
    private String appointmentId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected AppointmentSheetInboxJpaEntity() {
    }

    public AppointmentSheetInboxJpaEntity(String eventId, String appointmentId) {
        this.eventId = eventId;
        this.appointmentId = appointmentId;
    }

    @PrePersist
    void onCreate() {
        receivedAt = Instant.now();
    }
}
