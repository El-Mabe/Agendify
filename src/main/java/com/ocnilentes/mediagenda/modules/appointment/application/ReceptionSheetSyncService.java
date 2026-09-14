package com.ocnilentes.mediagenda.modules.appointment.application;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.AppointmentJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.AppointmentSheetInboxJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.JpaAppointmentRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.SpringDataAppointmentSheetInboxRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReceptionSheetSyncService {

    private final JpaAppointmentRepository appointmentRepository;
    private final SpringDataAppointmentSheetInboxRepository inboxRepository;
    private final AppointmentEventService appointmentEventService;

    public ReceptionSheetSyncService(
            JpaAppointmentRepository appointmentRepository,
            SpringDataAppointmentSheetInboxRepository inboxRepository,
            AppointmentEventService appointmentEventService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.inboxRepository = inboxRepository;
        this.appointmentEventService = appointmentEventService;
    }

    @Transactional
    public SyncResult apply(SheetRowChange change) {
        validate(change);
        if (inboxRepository.existsById(change.eventId())) {
            return new SyncResult(change.appointmentId(), "DUPLICATE");
        }
        boolean occupied = StringUtils.hasText(change.patientName()) || StringUtils.hasText(change.phone());
        Appointment existing = findExisting(change).orElse(null);

        if (!occupied) {
            if (existing == null) {
                inboxRepository.save(new AppointmentSheetInboxJpaEntity(change.eventId(), null));
                return new SyncResult(null, "IGNORED_EMPTY");
            }
            appointmentEventService.record(existing, AppointmentEventType.CANCELLED, existing.phone());
            appointmentRepository.delete(existing);
            inboxRepository.save(new AppointmentSheetInboxJpaEntity(change.eventId(), existing.id()));
            return new SyncResult(existing.id(), "CANCELLED");
        }

        Appointment appointment = new Appointment(
                existing == null ? resolveAppointmentId(change.appointmentId()) : existing.id(),
                change.date(),
                change.time(),
                change.consultorio(),
                StringUtils.hasText(change.patientName()) ? change.patientName().trim() : "Bloqueado por recepción",
                trimToNull(change.cedula()),
                StringUtils.hasText(change.phone()) ? change.phone().trim() : "",
                trimToNull(change.asistencia()),
                change.rowNumber()
        );
        Appointment saved = appointmentRepository.upsertFromSheet(
                appointment,
                change.sheetName(),
                change.rowNumber(),
                AppointmentJpaEntity.Origin.SHEET
        );
        if (existing == null) {
            appointmentEventService.record(saved, AppointmentEventType.SCHEDULED, saved.phone());
        }
        inboxRepository.save(new AppointmentSheetInboxJpaEntity(change.eventId(), saved.id()));
        return new SyncResult(saved.id(), existing == null ? "CREATED" : "UPDATED");
    }

    private java.util.Optional<Appointment> findExisting(SheetRowChange change) {
        if (StringUtils.hasText(change.appointmentId())) {
            java.util.Optional<Appointment> byId = appointmentRepository.findById(change.appointmentId());
            if (byId.isPresent()) {
                return byId;
            }
        }
        return appointmentRepository.findBySheetLocation(change.sheetName(), change.rowNumber());
    }

    private String resolveAppointmentId(String appointmentId) {
        if (StringUtils.hasText(appointmentId)) {
            try {
                return UUID.fromString(appointmentId.trim()).toString();
            } catch (IllegalArgumentException ignored) {
                // IDs anteriores o inválidos no se reutilizan.
            }
        }
        return UUID.randomUUID().toString();
    }

    private void validate(SheetRowChange change) {
        if (change == null
                || !StringUtils.hasText(change.sheetName())
                || !StringUtils.hasText(change.eventId())
                || change.rowNumber() < 1
                || change.date() == null
                || change.time() == null
                || change.consultorio() == null) {
            throw new IllegalArgumentException(
                    "sheetName, rowNumber, date, time y consultorio son obligatorios"
            );
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record SheetRowChange(
            String eventId,
            String appointmentId,
            String sheetName,
            int rowNumber,
            LocalDate date,
            LocalTime time,
            Consultorio consultorio,
            String patientName,
            String cedula,
            String phone,
            String asistencia
    ) {
    }

    public record SyncResult(String appointmentId, String action) {
    }
}
