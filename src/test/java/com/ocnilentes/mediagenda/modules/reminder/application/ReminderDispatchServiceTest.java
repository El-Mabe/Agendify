package com.ocnilentes.mediagenda.modules.reminder.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentService;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.AppointmentReminder;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import com.ocnilentes.mediagenda.modules.reminder.domain.port.AppointmentReminderRepository;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import com.ocnilentes.mediagenda.shared.config.ReminderProperties;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderDispatchServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Mock
    private AppointmentService appointmentService;
    @Mock
    private AppointmentReminderRepository reminderRepository;
    @Mock
    private WhatsappMessageClient whatsappMessageClient;

    private ReminderDispatchService service;

    @BeforeEach
    void setUp() {
        ReminderProperties properties = new ReminderProperties(
                true,
                0,
                23,
                "INTERACTIVE",
                "cita_recordatorio",
                "es_CO",
                "token"
        );
        service = new ReminderDispatchService(
                properties,
                appointmentService,
                reminderRepository,
                whatsappMessageClient
        );
    }

    @Test
    void shouldSendReminderOnceForTomorrowAppointments() {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        Appointment appointment = new Appointment(
                "A|" + tomorrow + "|10:00",
                tomorrow,
                LocalTime.of(10, 0),
                Consultorio.A,
                "Julian",
                "1234567890",
                "573000000001",
                null,
                12
        );

        when(appointmentService.listByDate(tomorrow)).thenReturn(List.of(appointment));
        when(reminderRepository.findByAppointmentKeyAndChannel(appointment.id(), ReminderChannel.DAY_BEFORE))
                .thenReturn(Optional.empty());
        when(whatsappMessageClient.sendInteractiveButtons(anyString(), anyString(), anyList()))
                .thenReturn("wamid.reminder");
        when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReminderDispatchService.ReminderRunResult result = service.dispatchDueReminders(true);

        assertEquals(1, result.sent());
        assertEquals(0, result.skipped());
        assertEquals(0, result.failed());

        ArgumentCaptor<AppointmentReminder> captor = ArgumentCaptor.forClass(AppointmentReminder.class);
        verify(reminderRepository).save(captor.capture());
        assertEquals(ReminderStatus.SENT, captor.getValue().status());
        assertEquals("wamid.reminder", captor.getValue().metaMessageId());
    }

    @Test
    void shouldSkipAlreadySentReminders() {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        Appointment appointment = new Appointment(
                "A|" + tomorrow + "|10:00",
                tomorrow,
                LocalTime.of(10, 0),
                Consultorio.A,
                "Julian",
                "1234567890",
                "573000000001",
                null,
                12
        );
        AppointmentReminder existing = new AppointmentReminder(
                1L,
                appointment.id(),
                ReminderChannel.DAY_BEFORE,
                appointment.phone(),
                appointment.patientName(),
                appointment.cedula(),
                appointment.date(),
                appointment.time(),
                ReminderStatus.SENT,
                "wamid.old",
                null,
                null,
                null
        );

        when(appointmentService.listByDate(tomorrow)).thenReturn(List.of(appointment));
        when(reminderRepository.findByAppointmentKeyAndChannel(appointment.id(), ReminderChannel.DAY_BEFORE))
                .thenReturn(Optional.of(existing));

        ReminderDispatchService.ReminderRunResult result = service.dispatchDueReminders(true);

        assertEquals(0, result.sent());
        assertEquals(1, result.skipped());
        verify(whatsappMessageClient, never()).sendInteractiveButtons(anyString(), anyString(), anyList());
    }

    @Test
    void shouldSkipReminderWhenPhoneIsAName() {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        Appointment appointment = new Appointment(
                "A|" + tomorrow + "|10:00",
                tomorrow,
                LocalTime.of(10, 0),
                Consultorio.A,
                "Julian",
                "1234567890",
                "Felipe",
                null,
                12
        );

        when(appointmentService.listByDate(tomorrow)).thenReturn(List.of(appointment));
        when(reminderRepository.findByAppointmentKeyAndChannel(appointment.id(), ReminderChannel.DAY_BEFORE))
                .thenReturn(Optional.empty());
        when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReminderDispatchService.ReminderRunResult result = service.dispatchDueReminders(true);

        assertEquals(0, result.sent());
        assertEquals(1, result.skipped());
        assertEquals(0, result.failed());
        verify(whatsappMessageClient, never()).sendInteractiveButtons(anyString(), anyString(), anyList());
        ArgumentCaptor<AppointmentReminder> captor = ArgumentCaptor.forClass(AppointmentReminder.class);
        verify(reminderRepository).save(captor.capture());
        assertEquals(ReminderStatus.FAILED, captor.getValue().status());
        assertTrue(captor.getValue().errorMessage().contains("Teléfono inválido"));
    }

    @Test
    void shouldNormalizeLocalMobileBeforeSendingToMeta() {
        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);
        Appointment appointment = new Appointment(
                "A|" + tomorrow + "|10:00",
                tomorrow,
                LocalTime.of(10, 0),
                Consultorio.A,
                "Julian",
                "1234567890",
                "3183108750",
                null,
                12
        );

        when(appointmentService.listByDate(tomorrow)).thenReturn(List.of(appointment));
        when(reminderRepository.findByAppointmentKeyAndChannel(appointment.id(), ReminderChannel.DAY_BEFORE))
                .thenReturn(Optional.empty());
        when(whatsappMessageClient.sendInteractiveButtons(anyString(), anyString(), anyList()))
                .thenReturn("wamid.reminder");
        when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReminderDispatchService.ReminderRunResult result = service.dispatchDueReminders(true);

        assertEquals(1, result.sent());
        verify(whatsappMessageClient).sendInteractiveButtons(
                eq("573183108750"),
                anyString(),
                anyList()
        );
    }

    @Test
    void shouldBuildConfirmAndCancelButtonIds() {
        assertTrue(ReminderPayloadFactory.isConfirm("rc|A|2026-08-22|10:00"));
        assertTrue(ReminderPayloadFactory.isCancel("rx|A|2026-08-22|10:00"));
        assertEquals("A|2026-08-22|10:00", ReminderPayloadFactory.extractAppointmentKey("rc|A|2026-08-22|10:00"));
    }
}
