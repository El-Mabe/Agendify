package com.ocnilentes.mediagenda.modules.appointment.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresAppointmentRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("appointments.sheet-sync.enabled", () -> "false");
    }

    @Autowired
    private JpaAppointmentRepository repository;

    @Autowired
    private SpringDataAppointmentRepository springData;

    @Autowired
    private SpringDataAppointmentSheetSyncRepository syncRepository;

    @BeforeEach
    void cleanDatabase() {
        syncRepository.deleteAll();
        springData.deleteAll();
    }

    @Test
    void databaseAllowsOnlyOneActiveAppointmentPerCedulaAndDayUnderConcurrency() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 10);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger saved = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> saveConcurrently(start, appointment(date, LocalTime.of(9, 0)), saved, rejected));
        executor.submit(() -> saveConcurrently(start, appointment(date, LocalTime.of(10, 0)), saved, rejected));
        start.countDown();
        executor.shutdown();

        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1, saved.get());
        assertEquals(1, rejected.get());
    }

    @Test
    void cancelledAppointmentReleasesDailyAndSlotConstraints() {
        LocalDate date = LocalDate.of(2026, 10, 11);
        Appointment first = repository.save(appointment(date, LocalTime.of(9, 0)));
        repository.delete(first);

        Appointment replacement = repository.save(appointment(date, LocalTime.of(9, 0)));

        assertEquals(replacement.id(), repository.findById(replacement.id()).orElseThrow().id());
    }

    @Test
    void outboxClaimIsAtomic() {
        Appointment appointment = repository.save(
                appointment(LocalDate.of(2026, 10, 12), LocalTime.of(9, 0))
        );
        AppointmentSheetSyncJpaEntity event = syncRepository.save(
                new AppointmentSheetSyncJpaEntity(
                        appointment.id(),
                        repository.findVersion(appointment.id()),
                        AppointmentSheetSyncJpaEntity.Operation.UPSERT
                )
        );
        Instant now = Instant.now();

        int firstClaim = syncRepository.claim(event.getId(), now, now.plusSeconds(300));
        int secondClaim = syncRepository.claim(event.getId(), now.plusSeconds(1), now.plusSeconds(301));

        assertEquals(1, firstClaim);
        assertEquals(0, secondClaim);
    }

    private void saveConcurrently(
            CountDownLatch start,
            Appointment appointment,
            AtomicInteger saved,
            AtomicInteger rejected
    ) {
        try {
            start.await();
            repository.save(appointment);
            saved.incrementAndGet();
        } catch (AppointmentException exception) {
            rejected.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private Appointment appointment(LocalDate date, LocalTime time) {
        return new Appointment(
                UUID.randomUUID().toString(),
                date,
                time,
                Consultorio.A,
                "Paciente",
                "1001001001",
                "573000000001",
                null,
                null
        );
    }
}
