package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointment_events")
class AppointmentEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_key", length = 64)
    private String appointmentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private AppointmentEventType eventType;

    @Column(name = "cedula", length = 20)
    private String cedula;

    @Column(name = "patient_name", length = 200)
    private String patientName;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "appointment_time")
    private LocalTime appointmentTime;

    @Column(name = "consultorio", length = 8)
    private String consultorio;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppointmentEventJpaEntity() {
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
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

    AppointmentEventType getEventType() {
        return eventType;
    }

    void setEventType(AppointmentEventType eventType) {
        this.eventType = eventType;
    }

    String getCedula() {
        return cedula;
    }

    void setCedula(String cedula) {
        this.cedula = cedula;
    }

    String getPatientName() {
        return patientName;
    }

    void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    String getPhone() {
        return phone;
    }

    void setPhone(String phone) {
        this.phone = phone;
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

    String getConsultorio() {
        return consultorio;
    }

    void setConsultorio(String consultorio) {
        this.consultorio = consultorio;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
