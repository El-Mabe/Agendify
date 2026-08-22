package com.ocnilentes.mediagenda.modules.appointment.outbound.memory;

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
    public Appointment save(Appointment appointment) {
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
