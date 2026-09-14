package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JpaAppointmentRepositoryTest {

    @Autowired
    private JpaAppointmentRepository repository;

    @Test
    void persistsQueriesAndSoftCancelsAppointment() {
        Appointment appointment = appointment("1001001001", LocalTime.of(9, 0));

        Appointment saved = repository.save(appointment);

        assertEquals(saved.id(), repository.findById(saved.id()).orElseThrow().id());
        assertEquals(1, repository.findByCedula("1001001001").size());
        assertTrue(repository.existsSlot(saved.date(), saved.time(), saved.consultorio()));

        repository.delete(saved);

        assertFalse(repository.findById(saved.id()).isPresent());
        assertFalse(repository.existsSlot(saved.date(), saved.time(), saved.consultorio()));
    }

    @Test
    void importsSameSheetRowIdempotently() {
        Appointment first = appointment("1001001002", LocalTime.of(10, 0));
        Appointment imported = repository.upsertFromSheet(
                first,
                "A_2026_09",
                12,
                AppointmentJpaEntity.Origin.IMPORT
        );

        Appointment updated = new Appointment(
                imported.id(),
                imported.date(),
                imported.time(),
                imported.consultorio(),
                "Nombre actualizado",
                imported.cedula(),
                imported.phone(),
                "ASISTIO",
                12
        );
        repository.upsertFromSheet(
                updated,
                "A_2026_09",
                12,
                AppointmentJpaEntity.Origin.IMPORT
        );

        Appointment stored = repository.findBySheetLocation("A_2026_09", 12).orElseThrow();
        assertEquals(imported.id(), stored.id());
        assertEquals("Nombre actualizado", stored.patientName());
        assertEquals("ASISTIO", stored.asistencia());
    }

    private Appointment appointment(String cedula, LocalTime time) {
        return new Appointment(
                UUID.randomUUID().toString(),
                LocalDate.now().plusDays(2),
                time,
                Consultorio.A,
                "Paciente",
                cedula,
                "573000000001",
                null,
                null
        );
    }
}
