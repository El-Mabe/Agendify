package com.ocnilentes.mediagenda.modules.appointment.domain.port;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    List<Appointment> findByPhone(String phone);

    List<Appointment> findByCedula(String cedula);

    List<Appointment> findByDate(LocalDate date);

    Optional<Appointment> findById(String id);

    boolean existsSlot(LocalDate date, LocalTime time, Consultorio consultorio);

    long countByDateAndConsultorio(LocalDate date, Consultorio consultorio);

    void delete(Appointment appointment);

    void updateAsistencia(Appointment appointment, String asistencia);

    Set<LocalDate> findHolidays();
}
