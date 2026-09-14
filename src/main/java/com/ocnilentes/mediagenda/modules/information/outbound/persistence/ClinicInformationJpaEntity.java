package com.ocnilentes.mediagenda.modules.information.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "clinic_information")
public class ClinicInformationJpaEntity {

    @Id
    @Column(name = "info_key", length = 100, nullable = false)
    private String key;

    @Column(name = "info_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    protected ClinicInformationJpaEntity() {
    }

    public ClinicInformationJpaEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @PrePersist
    @PreUpdate
    void updateSyncedAt() {
        syncedAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
