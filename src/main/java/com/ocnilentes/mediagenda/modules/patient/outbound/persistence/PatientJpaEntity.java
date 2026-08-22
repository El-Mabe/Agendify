package com.ocnilentes.mediagenda.modules.patient.outbound.persistence;

import com.ocnilentes.mediagenda.modules.patient.domain.model.FollowUpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
class PatientJpaEntity {

    @Id
    @Column(name = "cedula", length = 20, nullable = false)
    private String cedula;

    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;

    @Column(name = "last_phone", length = 30, nullable = false)
    private String lastPhone;

    @Column(name = "last_attended_at")
    private LocalDate lastAttendedAt;

    @Column(name = "next_follow_up_on")
    private LocalDate nextFollowUpOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_status", nullable = false, length = 32)
    private FollowUpStatus followUpStatus = FollowUpStatus.NONE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientJpaEntity() {
    }

    PatientJpaEntity(String cedula, String fullName, String lastPhone) {
        this.cedula = cedula;
        this.fullName = fullName;
        this.lastPhone = lastPhone;
        this.followUpStatus = FollowUpStatus.NONE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (followUpStatus == null) {
            followUpStatus = FollowUpStatus.NONE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    String getCedula() {
        return cedula;
    }

    String getFullName() {
        return fullName;
    }

    String getLastPhone() {
        return lastPhone;
    }

    LocalDate getLastAttendedAt() {
        return lastAttendedAt;
    }

    LocalDate getNextFollowUpOn() {
        return nextFollowUpOn;
    }

    FollowUpStatus getFollowUpStatus() {
        return followUpStatus;
    }

    void setFullName(String fullName) {
        this.fullName = fullName;
    }

    void setLastPhone(String lastPhone) {
        this.lastPhone = lastPhone;
    }

    void setLastAttendedAt(LocalDate lastAttendedAt) {
        this.lastAttendedAt = lastAttendedAt;
    }

    void setNextFollowUpOn(LocalDate nextFollowUpOn) {
        this.nextFollowUpOn = nextFollowUpOn;
    }

    void setFollowUpStatus(FollowUpStatus followUpStatus) {
        this.followUpStatus = followUpStatus;
    }
}
