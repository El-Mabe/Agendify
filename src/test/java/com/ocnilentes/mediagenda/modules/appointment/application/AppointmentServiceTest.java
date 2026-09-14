package com.ocnilentes.mediagenda.modules.appointment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ocnilentes.mediagenda.modules.appointment.domain.exception.AppointmentException;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AppointmentEventType;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.AttendanceStatus;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.appointment.outbound.memory.InMemoryAppointmentRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private InMemoryAppointmentRepository repository;
    private AppointmentEventService appointmentEventService;
    private AppointmentSheetSyncService appointmentSheetSyncService;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAppointmentRepository();
        appointmentEventService = mock(AppointmentEventService.class);
        appointmentSheetSyncService = mock(AppointmentSheetSyncService.class);
        appointmentService = new AppointmentService(
                repository,
                appointmentEventService,
                appointmentSheetSyncService
        );
    }

    @Test
    void shouldListAvailableDatesExcludingTodayAndSunday() {
        List<LocalDate> dates = appointmentService.listAvailableDates();
        assertFalse(dates.isEmpty());
        assertTrue(dates.size() <= 5);

        LocalDate today = LocalDate.now(ZONE);
        for (LocalDate date : dates) {
            assertTrue(date.isAfter(today));
            assertTrue(date.getDayOfWeek() != DayOfWeek.SUNDAY);
        }
    }

    @Test
    void shouldAssignConsultorioWithFewerAppointments() {
        LocalDate date = nextWeekday(LocalDate.now(ZONE).plusDays(1));
        LocalTime time = LocalTime.of(10, 0);

        Appointment first = appointmentService.schedule(
                "573000000001", "Paciente Uno", "1001001001", date, time
        );
        assertEquals(Consultorio.A, first.consultorio());
        assertEquals("1001001001", first.cedula());
        verify(appointmentEventService).record(any(), eq(AppointmentEventType.SCHEDULED), eq("573000000001"));

        Appointment second = appointmentService.schedule(
                "573000000002", "Paciente Dos", "1001001002", date, time
        );
        assertEquals(Consultorio.B, second.consultorio());
    }

    @Test
    void shouldCancelByCedulaAndRecordEvent() {
        LocalDate date = nextWeekday(LocalDate.now(ZONE).plusDays(1));
        Appointment scheduled = appointmentService.schedule(
                "573043370408",
                "Julian",
                "1234567890",
                date,
                LocalTime.of(9, 0)
        );

        appointmentService.cancel(scheduled.id(), "1234567890");
        assertTrue(appointmentService.listByCedula("1234567890").isEmpty());
        verify(appointmentEventService).record(any(), eq(AppointmentEventType.CANCELLED), eq("573043370408"));
    }

    @Test
    void shouldMarkConfirmedInAsistencia() {
        LocalDate date = nextWeekday(LocalDate.now(ZONE).plusDays(1));
        Appointment scheduled = appointmentService.schedule(
                "573043370408",
                "Julian",
                "1234567890",
                date,
                LocalTime.of(9, 0)
        );

        Appointment confirmed = appointmentService.markConfirmed(scheduled.id(), "573043370408");
        assertEquals(AttendanceStatus.CONFIRMO.name(), confirmed.asistencia());
        assertEquals(
                AttendanceStatus.CONFIRMO.name(),
                repository.findById(scheduled.id()).orElseThrow().asistencia()
        );
        verify(appointmentEventService).record(any(), eq(AppointmentEventType.CONFIRMED), eq("573043370408"));
    }

    @Test
    void shouldRejectSameDayBooking() {
        LocalDate today = LocalDate.now(ZONE);
        assertThrows(
                AppointmentException.class,
                () -> appointmentService.schedule(
                        "573043370408",
                        "Julian",
                        "1234567890",
                        today,
                        LocalTime.of(10, 0)
                )
        );
    }

    @Test
    void shouldRejectSecondAppointmentSameDayForSameCedula() {
        LocalDate date = nextWeekday(LocalDate.now(ZONE).plusDays(1));

        appointmentService.schedule(
                "573043370408",
                "Julian",
                "1234567890",
                date,
                LocalTime.of(9, 0)
        );

        AppointmentException exception = assertThrows(
                AppointmentException.class,
                () -> appointmentService.schedule(
                        "573043370408",
                        "Julian",
                        "1234567890",
                        date,
                        LocalTime.of(11, 0)
                )
        );

        assertTrue(exception.getMessage().contains("Ya tienes una cita reservada para ese día"));
        assertEquals(1, appointmentService.listByCedula("1234567890").size());
    }

    @Test
    void shouldKeepOnlyOneAppointmentWhenSameCedulaBooksConcurrently() throws Exception {
        LocalDate date = nextWeekday(LocalDate.now(ZONE).plusDays(1));
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> scheduleConcurrently(start, successes, rejected, date, LocalTime.of(9, 0)));
        executor.submit(() -> scheduleConcurrently(start, successes, rejected, date, LocalTime.of(10, 0)));
        start.countDown();
        executor.shutdown();

        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, successes.get());
        assertEquals(1, rejected.get());
        assertEquals(1, appointmentService.listByCedula("1234567890").size());
    }

    private void scheduleConcurrently(
            CountDownLatch start,
            AtomicInteger successes,
            AtomicInteger rejected,
            LocalDate date,
            LocalTime time
    ) {
        try {
            start.await();
            appointmentService.schedule(
                    "573043370408",
                    "Julian",
                    "1234567890",
                    date,
                    time
            );
            successes.incrementAndGet();
        } catch (AppointmentException exception) {
            rejected.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private LocalDate nextWeekday(LocalDate start) {
        LocalDate date = start;
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
