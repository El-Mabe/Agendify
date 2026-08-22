package com.ocnilentes.mediagenda.modules.reminder.outbound.persistence;

import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "appointment_reminders",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_appointment_reminders_key_channel",
                columnNames = {"appointment_key", "channel"}
        )
)
class AppointmentReminderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_key", nullable = false, length = 64)
    private String appointmentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private ReminderChannel channel;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "patient_name", nullable = false, length = 200)
    private String patientName;

    @Column(name = "cedula", length = 20)
    private String cedula;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReminderStatus status;

    @Column(name = "meta_message_id", length = 128)
    private String metaMessageId;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppointmentReminderJpaEntity() {
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    String getAppointmentKey() {
        return appointmentKey;
    }

    void setAppointmentKey(String appointmentKey) {
        this.appointmentKey = appointmentKey;
    }

    ReminderChannel getChannel() {
        return channel;
    }

    void setChannel(ReminderChannel channel) {
        this.channel = channel;
    }

    String getPhone() {
        return phone;
    }

    void setPhone(String phone) {
        this.phone = phone;
    }

    String getPatientName() {
        return patientName;
    }

    void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    String getCedula() {
        return cedula;
    }

    void setCedula(String cedula) {
        this.cedula = cedula;
    }

    LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    ReminderStatus getStatus() {
        return status;
    }

    void setStatus(ReminderStatus status) {
        this.status = status;
    }

    String getMetaMessageId() {
        return metaMessageId;
    }

    void setMetaMessageId(String metaMessageId) {
        this.metaMessageId = metaMessageId;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    Instant getSentAt() {
        return sentAt;
    }

    void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    Instant getRespondedAt() {
        return respondedAt;
    }

    void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}
