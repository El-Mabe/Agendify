package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.domain.port.AppointmentRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JpaAppointmentRepository implements AppointmentRepository {

    private static final AppointmentJpaEntity.Status ACTIVE = AppointmentJpaEntity.Status.SCHEDULED;

    private final SpringDataAppointmentRepository springData;
    private final SpringDataClinicHolidayRepository holidayRepository;

    public JpaAppointmentRepository(
            SpringDataAppointmentRepository springData,
            SpringDataClinicHolidayRepository holidayRepository
    ) {
        this.springData = springData;
        this.holidayRepository = holidayRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentJpaEntity entity = springData.findById(appointment.id())
                .orElseGet(AppointmentJpaEntity::new);
        entity.setId(appointment.id());
        copyAppointment(entity, appointment);
        entity.setStatus(ACTIVE);
        if (entity.getOrigin() == null) {
            entity.setOrigin(AppointmentJpaEntity.Origin.BOT);
        }
        try {
            return toDomain(springData.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw translateConstraintViolation(appointment, exception);
        }
    }

    @Override
    public List<Appointment> findByPhone(String phone) {
        return springData.findByPhoneAndStatusOrderByDateAscTimeAsc(phone, ACTIVE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findByCedula(String cedula) {
        return springData.findByCedulaAndStatusOrderByDateAscTimeAsc(cedula, ACTIVE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Appointment> findByCedulaAndDate(String cedula, LocalDate date) {
        return springData.findFirstByCedulaAndDateAndStatus(cedula, date, ACTIVE)
                .map(this::toDomain);
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return springData.findByDateAndStatusOrderByTimeAsc(date, ACTIVE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findByAsistencia(String asistencia) {
        return springData.findByAsistenciaAndStatusOrderByDateAscTimeAsc(asistencia, ACTIVE).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return springData.findByIdAndStatus(id, ACTIVE)
                .or(() -> springData.findByLegacyKeyAndStatus(id, ACTIVE))
                .map(this::toDomain);
    }

    @Override
    public boolean existsSlot(LocalDate date, LocalTime time, Consultorio consultorio) {
        return springData.existsByDateAndTimeAndConsultorioAndStatus(date, time, consultorio, ACTIVE);
    }

    @Override
    public long countByDateAndConsultorio(LocalDate date, Consultorio consultorio) {
        return springData.countByDateAndConsultorioAndStatus(date, consultorio, ACTIVE);
    }

    @Override
    public void delete(Appointment appointment) {
        AppointmentJpaEntity entity = springData.findById(appointment.id())
                .orElseThrow(() -> new AppointmentException("Cita no encontrada"));
        entity.setStatus(AppointmentJpaEntity.Status.CANCELLED);
        springData.saveAndFlush(entity);
    }

    @Override
    public void updateAsistencia(Appointment appointment, String asistencia) {
        AppointmentJpaEntity entity = springData.findByIdAndStatus(appointment.id(), ACTIVE)
                .orElseThrow(() -> new AppointmentException("Cita no encontrada"));
        entity.setAsistencia(asistencia);
        springData.saveAndFlush(entity);
    }

    @Override
    public Set<LocalDate> findHolidays() {
        return holidayRepository.findAll().stream()
                .map(ClinicHolidayJpaEntity::getDate)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Appointment upsertFromSheet(
            Appointment appointment,
            String sheetName,
            int rowNumber,
            AppointmentJpaEntity.Origin origin
    ) {
        Optional<AppointmentJpaEntity> byStableId = isUuid(appointment.id())
                ? springData.findByIdAndStatus(appointment.id(), ACTIVE)
                : Optional.empty();
        AppointmentJpaEntity entity = byStableId
                .or(() -> springData.findBySheetNameAndSheetRowNumberAndStatus(
                        sheetName,
                        rowNumber,
                        ACTIVE
                ))
                .orElseGet(AppointmentJpaEntity::new);
        if (!StringUtils.hasText(entity.getId())) {
            if (isUuid(appointment.id())) {
                entity.setId(appointment.id());
            } else {
                entity.setId(UUID.randomUUID().toString());
                entity.setLegacyKey(appointment.id());
            }
        }
        copyAppointment(entity, appointment);
        entity.setSheetName(sheetName);
        entity.setSheetRowNumber(rowNumber);
        entity.setOrigin(origin);
        entity.setStatus(ACTIVE);
        try {
            return toDomain(springData.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw translateConstraintViolation(appointment, exception);
        }
    }

    public Optional<Appointment> findBySheetLocation(String sheetName, int rowNumber) {
        return springData.findBySheetNameAndSheetRowNumberAndStatus(sheetName, rowNumber, ACTIVE)
                .map(this::toDomain);
    }

    public List<Appointment> findActiveBySheetName(String sheetName) {
        return springData.findBySheetNameAndStatus(sheetName, ACTIVE).stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<Appointment> findAnyById(String id) {
        return springData.findById(id).map(this::toDomain);
    }

    public long findVersion(String id) {
        return springData.findById(id)
                .map(AppointmentJpaEntity::getVersion)
                .orElseThrow(() -> new AppointmentException("Cita no encontrada"));
    }

    public void updateSheetLocation(String appointmentId, String sheetName, int rowNumber) {
        AppointmentJpaEntity entity = springData.findById(appointmentId)
                .orElseThrow(() -> new AppointmentException("Cita no encontrada"));
        entity.setSheetName(sheetName);
        entity.setSheetRowNumber(rowNumber);
        springData.saveAndFlush(entity);
    }

    private void copyAppointment(AppointmentJpaEntity entity, Appointment appointment) {
        entity.setDate(appointment.date());
        entity.setTime(appointment.time());
        entity.setConsultorio(appointment.consultorio());
        entity.setPatientName(appointment.patientName());
        entity.setCedula(appointment.cedula());
        entity.setPhone(appointment.phone());
        entity.setAsistencia(appointment.asistencia());
        if (appointment.sheetRowNumber() != null) {
            entity.setSheetRowNumber(appointment.sheetRowNumber());
        }
    }

    private Appointment toDomain(AppointmentJpaEntity entity) {
        return new Appointment(
                entity.getId(),
                entity.getDate(),
                entity.getTime(),
                entity.getConsultorio(),
                entity.getPatientName(),
                entity.getCedula(),
                entity.getPhone(),
                entity.getAsistencia(),
                entity.getSheetRowNumber()
        );
    }

    private AppointmentException translateConstraintViolation(
            Appointment appointment,
            DataIntegrityViolationException exception
    ) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uq_appointments_active_cedula_date")) {
            return new AppointmentException(
                    "😊 Ya tienes una cita reservada para ese día. "
                            + "Para poder atenderte de la mejor manera, solo puedes tener una cita por día."
            );
        }
        if (message != null && message.contains("uq_appointments_active_slot")) {
            return new AppointmentException("Ese horario acabó de ocuparse. Elige otra hora.");
        }
        return new AppointmentException(
                "No pudimos guardar la cita en este momento. Intenta nuevamente.",
                exception
        );
    }

    private boolean isUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
