package com.ocnilentes.mediagenda.modules.appointment.application;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AttendanceStatus;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.AttendanceSyncJpaEntity;
import com.ocnilentes.mediagenda.modules.appointment.outbound.persistence.SpringDataAttendanceSyncRepository;
import com.ocnilentes.mediagenda.modules.patient.application.PatientService;
import com.ocnilentes.mediagenda.shared.config.AttendanceSyncProperties;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AttendanceSyncService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSyncService.class);
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Bogota");

    private final AttendanceSyncProperties properties;
    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final AppointmentEventService appointmentEventService;
    private final SpringDataAttendanceSyncRepository attendanceSyncRepository;

    public AttendanceSyncService(
            AttendanceSyncProperties properties,
            AppointmentService appointmentService,
            PatientService patientService,
            AppointmentEventService appointmentEventService,
            SpringDataAttendanceSyncRepository attendanceSyncRepository
    ) {
        this.properties = properties;
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.appointmentEventService = appointmentEventService;
        this.attendanceSyncRepository = attendanceSyncRepository;
    }

    public AttendanceSyncResult syncAttendedAppointments() {
        return syncAttendedAppointments(false);
    }

    public AttendanceSyncResult syncAttendedAppointments(boolean ignoreWindow) {
        if (!properties.enabled()) {
            return AttendanceSyncResult.disabled();
        }

        LocalTime now = LocalTime.now(CLINIC_ZONE);
        if (!ignoreWindow
                && (now.getHour() < properties.windowStartHour() || now.getHour() > properties.windowEndHour())) {
            return AttendanceSyncResult.outsideWindow();
        }

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        List<Appointment> appointments = appointmentService.listByAttendance(AttendanceStatus.ASISTIO);
        for (Appointment appointment : appointments) {
            if (!StringUtils.hasText(appointment.cedula())) {
                skipped++;
                continue;
            }
            if (attendanceSyncRepository.existsById(appointment.id())
                    || appointmentEventService.alreadyAttended(appointment.id())) {
                skipped++;
                continue;
            }

            try {
                patientService.markAttended(
                        appointment.cedula(),
                        appointment.patientName() == null ? "Paciente" : appointment.patientName(),
                        appointment.phone(),
                        appointment.date()
                );
                appointmentEventService.record(
                        appointment,
                        AppointmentEventType.ATTENDED,
                        appointment.phone()
                );
                attendanceSyncRepository.save(new AttendanceSyncJpaEntity(
                        appointment.id(),
                        appointment.date(),
                        appointment.cedula()
                ));
                processed++;
            } catch (RuntimeException exception) {
                failed++;
                String message = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                errors.add(appointment.id() + ": " + message);
                log.error("Failed attendance sync for appointment={}", appointment.id(), exception);
            }
        }

        log.info(
                "Attendance sync finished processed={} skipped={} failed={}",
                processed,
                skipped,
                failed
        );
        return new AttendanceSyncResult(true, true, processed, skipped, failed, errors);
    }

    public record AttendanceSyncResult(
            boolean enabled,
            boolean insideWindow,
            int processed,
            int skipped,
            int failed,
            List<String> errors
    ) {
        static AttendanceSyncResult disabled() {
            return new AttendanceSyncResult(false, false, 0, 0, 0, List.of());
        }

        static AttendanceSyncResult outsideWindow() {
            return new AttendanceSyncResult(true, false, 0, 0, 0, List.of());
        }
    }
}
