package com.ocnilentes.mediagenda.modules.appointment.application;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AttendanceStatus;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.ClinicSchedule;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.domain.port.AppointmentRepository;
import com.ocnilentes.mediagenda.modules.patient.application.CedulaValidator;
import com.ocnilentes.mediagenda.modules.patient.domain.exception.PatientException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AppointmentService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_AVAILABLE_DATES = 5;
    private static final int MAX_SEARCH_DAYS = 60;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentEventService appointmentEventService;
    private final AppointmentSheetSyncService appointmentSheetSyncService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentEventService appointmentEventService,
            AppointmentSheetSyncService appointmentSheetSyncService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentEventService = appointmentEventService;
        this.appointmentSheetSyncService = appointmentSheetSyncService;
    }

    public List<LocalDate> listAvailableDates() {
        LocalDate today = LocalDate.now(CLINIC_ZONE);
        Set<LocalDate> holidays = appointmentRepository.findHolidays();
        List<LocalDate> available = new ArrayList<>();

        for (int offset = 1; offset <= MAX_SEARCH_DAYS && available.size() < MAX_AVAILABLE_DATES; offset++) {
            LocalDate candidate = today.plusDays(offset);
            if (!isBookableDay(candidate, holidays)) {
                continue;
            }
            if (!listAvailableTimes(candidate, holidays).isEmpty()) {
                available.add(candidate);
            }
        }
        return available;
    }

    public List<LocalTime> listAvailableTimes(LocalDate date) {
        return listAvailableTimes(date, appointmentRepository.findHolidays());
    }

    @Transactional
    public Appointment schedule(
            String phone,
            String patientName,
            String cedula,
            LocalDate date,
            LocalTime time
    ) {
        requireText(phone, "Phone is required");
        requireText(patientName, "Patient name is required");
        String normalizedCedula;
        try {
            normalizedCedula = CedulaValidator.normalizeAndValidate(cedula);
        } catch (PatientException exception) {
            throw new AppointmentException(exception.getMessage());
        }
        if (date == null || time == null) {
            throw new AppointmentException("Date and time are required");
        }

        LocalDate today = LocalDate.now(CLINIC_ZONE);
        if (!date.isAfter(today)) {
            throw new AppointmentException("No se pueden agendar citas para hoy. Elige una fecha a partir de mañana.");
        }

        Optional<Appointment> existingSameDay =
                appointmentRepository.findByCedulaAndDate(normalizedCedula, date);
        if (existingSameDay.isPresent()) {
            Appointment existing = existingSameDay.get();
            throw new AppointmentException(
                    "😊 Ya tienes una cita reservada para ese día a las "
                            + existing.time().format(TIME_FORMAT)
                            + ". Para poder atenderte de la mejor manera, solo puedes tener una cita por día. "
                            + "Puedes elegir otra fecha con gusto."
            );
        }

        Set<LocalDate> holidays = appointmentRepository.findHolidays();
        if (!isBookableDay(date, holidays)) {
            throw new AppointmentException("Esa fecha no está disponible (domingo o festivo).");
        }

        if (!listAvailableTimes(date, holidays).contains(time)) {
            throw new AppointmentException("Ese horario ya no está disponible. Elige otra hora.");
        }
        Consultorio consultorio = chooseConsultorio(date, time);
        Appointment appointment = new Appointment(
                UUID.randomUUID().toString(),
                date,
                time,
                consultorio,
                patientName.trim(),
                normalizedCedula,
                phone.trim(),
                null,
                null
        );
        Appointment saved = appointmentRepository.save(appointment);
        appointmentEventService.record(saved, AppointmentEventType.SCHEDULED, phone.trim());
        appointmentSheetSyncService.syncCreated(saved);
        return saved;
    }

    public List<Appointment> listByPhone(String phone) {
        requireText(phone, "Phone is required");
        return appointmentRepository.findByPhone(phone.trim()).stream()
                .sorted(Comparator.comparing(Appointment::date).thenComparing(Appointment::time))
                .toList();
    }

    public List<Appointment> listByCedula(String cedula) {
        String normalizedCedula;
        try {
            normalizedCedula = CedulaValidator.normalizeAndValidate(cedula);
        } catch (PatientException exception) {
            throw new AppointmentException(exception.getMessage());
        }
        return appointmentRepository.findByCedula(normalizedCedula).stream()
                .sorted(Comparator.comparing(Appointment::date).thenComparing(Appointment::time))
                .toList();
    }

    public Optional<Appointment> findByCedulaAndDate(String cedula, LocalDate date) {
        String normalizedCedula;
        try {
            normalizedCedula = CedulaValidator.normalizeAndValidate(cedula);
        } catch (PatientException exception) {
            throw new AppointmentException(exception.getMessage());
        }
        if (date == null) {
            throw new AppointmentException("Date is required");
        }
        return appointmentRepository.findByCedulaAndDate(normalizedCedula, date);
    }

    @Transactional
    public Appointment cancel(String appointmentId, String cedula) {
        requireText(appointmentId, "Appointment id is required");
        String normalizedCedula;
        try {
            normalizedCedula = CedulaValidator.normalizeAndValidate(cedula);
        } catch (PatientException exception) {
            throw new AppointmentException(exception.getMessage());
        }

        Appointment appointment = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new AppointmentException("Cita no encontrada"));

        if (appointment.cedula() == null || !appointment.cedula().equals(normalizedCedula)) {
            throw new AppointmentException("La cita no pertenece a esta cédula");
        }

        return cancelInternal(appointment, appointment.phone());
    }

    public List<Appointment> listByDate(LocalDate date) {
        if (date == null) {
            throw new AppointmentException("Date is required");
        }
        return appointmentRepository.findByDate(date).stream()
                .sorted(Comparator.comparing(Appointment::time))
                .toList();
    }

    public List<Appointment> listByAttendance(AttendanceStatus status) {
        if (status == null) {
            throw new AppointmentException("Attendance status is required");
        }
        return appointmentRepository.findByAsistencia(status.name());
    }

    @Transactional
    public Appointment cancelByPhone(String appointmentId, String phone) {
        requireText(appointmentId, "Appointment id is required");
        requireText(phone, "Phone is required");

        Appointment appointment = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new AppointmentException("Cita no encontrada"));

        if (!appointment.phone().equals(phone.trim())) {
            throw new AppointmentException("La cita no pertenece a este número");
        }

        return cancelInternal(appointment, phone.trim());
    }

    @Transactional
    public Appointment markConfirmed(String appointmentId, String actorPhone) {
        requireText(appointmentId, "Appointment id is required");
        Appointment appointment = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new AppointmentException("Cita no encontrada"));

        appointmentRepository.updateAsistencia(appointment, AttendanceStatus.CONFIRMO.name());
        appointmentEventService.record(appointment, AppointmentEventType.CONFIRMED, actorPhone);
        Appointment confirmed = appointment.withAsistencia(AttendanceStatus.CONFIRMO.name());
        appointmentSheetSyncService.syncAttendance(confirmed);
        return confirmed;
    }

    private Appointment cancelInternal(Appointment appointment, String actorPhone) {
        // Auditoría en BD (cédula + teléfono que canceló) antes de liberar el slot en Sheets.
        appointmentEventService.record(appointment, AppointmentEventType.CANCELLED, actorPhone);
        appointmentRepository.delete(appointment);
        appointmentSheetSyncService.syncCancelled(appointment);
        return appointment;
    }

    private List<LocalTime> listAvailableTimes(LocalDate date, Set<LocalDate> holidays) {
        if (date == null) {
            throw new AppointmentException("Date is required");
        }
        if (!isBookableDay(date, holidays)) {
            return List.of();
        }

        Set<LocalTime> available = new LinkedHashSet<>();
        for (Consultorio consultorio : Consultorio.values()) {
            for (LocalTime slot : ClinicSchedule.slotsFor(consultorio)) {
                if (!appointmentRepository.existsSlot(date, slot, consultorio)) {
                    available.add(slot);
                }
            }
        }

        return available.stream().sorted().toList();
    }

    private Consultorio chooseConsultorio(LocalDate date, LocalTime time) {
        List<Consultorio> candidates = new ArrayList<>();
        for (Consultorio consultorio : Consultorio.values()) {
            if (ClinicSchedule.offers(consultorio, time)
                    && !appointmentRepository.existsSlot(date, time, consultorio)) {
                candidates.add(consultorio);
            }
        }
        if (candidates.isEmpty()) {
            throw new AppointmentException("No hay consultorio disponible para esa hora");
        }

        return candidates.stream()
                .min(Comparator.comparingLong(consultorio ->
                        appointmentRepository.countByDateAndConsultorio(date, consultorio)))
                .orElseThrow();
    }

    private boolean isBookableDay(LocalDate date, Set<LocalDate> holidays) {
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        return holidays == null || !holidays.contains(date);
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new AppointmentException(message);
        }
    }
}
