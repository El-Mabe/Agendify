package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class AppointmentJpaEntity {

    public enum Status {
        SCHEDULED,
        CANCELLED
    }

    public enum Origin {
        BOT,
        SHEET,
        IMPORT
    }

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate date;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultorio", length = 8, nullable = false)
    private Consultorio consultorio;

    @Column(name = "patient_name", length = 200, nullable = false)
    private String patientName;

    @Column(name = "cedula", length = 20)
    private String cedula;

    @Column(name = "phone", length = 30, nullable = false)
    private String phone;

    @Column(name = "asistencia", length = 32)
    private String asistencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", length = 20, nullable = false)
    private Origin origin = Origin.BOT;

    @Column(name = "sheet_name", length = 100)
    private String sheetName;

    @Column(name = "sheet_row_number")
    private Integer sheetRowNumber;

    @Column(name = "legacy_key", length = 64)
    private String legacyKey;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppointmentJpaEntity() {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Consultorio getConsultorio() {
        return consultorio;
    }

    public void setConsultorio(Consultorio consultorio) {
        this.consultorio = consultorio;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAsistencia() {
        return asistencia;
    }

    public void setAsistencia(String asistencia) {
        this.asistencia = asistencia;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public Integer getSheetRowNumber() {
        return sheetRowNumber;
    }

    public void setSheetRowNumber(Integer sheetRowNumber) {
        this.sheetRowNumber = sheetRowNumber;
    }

    public String getLegacyKey() {
        return legacyKey;
    }

    public void setLegacyKey(String legacyKey) {
        this.legacyKey = legacyKey;
    }

    public long getVersion() {
        return version;
    }
}
