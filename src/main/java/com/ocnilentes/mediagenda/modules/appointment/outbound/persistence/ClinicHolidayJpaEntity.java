package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "clinic_holidays")
public class ClinicHolidayJpaEntity {

    @Id
    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected ClinicHolidayJpaEntity() {
    }

    public ClinicHolidayJpaEntity(LocalDate date) {
        this.date = date;
    }

    @PrePersist
    @PreUpdate
    void updateSyncedAt() {
        syncedAt = Instant.now();
    }

    public LocalDate getDate() {
        return date;
    }
}
