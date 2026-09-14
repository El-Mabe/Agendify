package com.ocnilentes.mediagenda.modules.appointment.outbound.memory;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.domain.port.AppointmentRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAppointmentRepository implements AppointmentRepository {

    private final Map<String, Appointment> appointments = new ConcurrentHashMap<>();
    private final Set<LocalDate> holidays = ConcurrentHashMap.newKeySet();

    @Override
    public synchronized Appointment save(Appointment appointment) {
        boolean samePatientAndDay = appointments.values().stream().anyMatch(existing ->
                appointment.cedula() != null
                        && appointment.cedula().equals(existing.cedula())
                        && appointment.date().equals(existing.date())
        );
        if (samePatientAndDay) {
            throw new AppointmentException("Solo se permite una cita por cédula y día.");
        }
        boolean occupiedSlot = appointments.values().stream().anyMatch(existing ->
                appointment.date().equals(existing.date())
                        && appointment.time().equals(existing.time())
                        && appointment.consultorio() == existing.consultorio()
        );
        if (occupiedSlot) {
            throw new AppointmentException("Ese horario acabó de ocuparse. Elige otra hora.");
        }
        appointments.put(appointment.id(), appointment);
        return appointment;
    }

    @Override
    public List<Appointment> findByPhone(String phone) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment appointment : appointments.values()) {
            if (appointment.phone().equals(phone)) {
                result.add(appointment);
            }
        }
        return result;
    }

    @Override
    public List<Appointment> findByCedula(String cedula) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment appointment : appointments.values()) {
            if (cedula != null && cedula.equals(appointment.cedula())) {
                result.add(appointment);
            }
        }
        return result;
    }

    @Override
    public Optional<Appointment> findByCedulaAndDate(String cedula, LocalDate date) {
        return appointments.values().stream()
                .filter(appointment -> cedula != null && cedula.equals(appointment.cedula()))
                .filter(appointment -> date != null && date.equals(appointment.date()))
                .findFirst();
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment appointment : appointments.values()) {
            if (appointment.date().equals(date)) {
                result.add(appointment);
            }
        }
        return result;
    }

    @Override
    public List<Appointment> findByAsistencia(String asistencia) {
        return appointments.values().stream()
                .filter(appointment -> asistencia != null && asistencia.equals(appointment.asistencia()))
                .toList();
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return Optional.ofNullable(appointments.get(id));
    }

    @Override
    public boolean existsSlot(LocalDate date, LocalTime time, Consultorio consultorio) {
        return appointments.values().stream().anyMatch(appointment ->
                appointment.date().equals(date)
                        && appointment.time().equals(time)
                        && appointment.consultorio() == consultorio
        );
    }

    @Override
    public long countByDateAndConsultorio(LocalDate date, Consultorio consultorio) {
        return appointments.values().stream()
                .filter(appointment -> appointment.date().equals(date))
                .filter(appointment -> appointment.consultorio() == consultorio)
                .count();
    }

    @Override
    public void delete(Appointment appointment) {
        appointments.remove(appointment.id());
    }

    @Override
    public void updateAsistencia(Appointment appointment, String asistencia) {
        Appointment current = appointments.get(appointment.id());
        if (current != null) {
            appointments.put(appointment.id(), current.withAsistencia(asistencia));
        }
    }

    @Override
    public Set<LocalDate> findHolidays() {
        return Set.copyOf(holidays);
    }

    public void addHoliday(LocalDate date) {
        holidays.add(date);
    }
}
