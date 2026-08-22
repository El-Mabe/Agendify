package com.ocnilentes.mediagenda.modules.reminder.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentService;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Appointment;
import com.ocnilentes.mediagenda.modules.appointment.domain.model.Consultorio;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.AppointmentReminder;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderChannel;
import com.ocnilentes.mediagenda.modules.reminder.domain.model.ReminderStatus;
import com.ocnilentes.mediagenda.modules.reminder.domain.port.AppointmentReminderRepository;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingButtonReply;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderResponseServiceTest {

    @Mock
    private AppointmentReminderRepository reminderRepository;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private WhatsappMessageClient whatsappMessageClient;

    private ReminderResponseService service;

    @BeforeEach
    void setUp() {
        service = new ReminderResponseService(reminderRepository, appointmentService, whatsappMessageClient);
    }

    @Test
    void shouldConfirmReminderFromButton() {
        LocalDate date = LocalDate.of(2026, 8, 22);
        AppointmentReminder reminder = new AppointmentReminder(
                1L,
                "A|2026-08-22|10:00",
                ReminderChannel.DAY_BEFORE,
                "573000000001",
                "Julian",
                "123",
                date,
                LocalTime.of(10, 0),
                ReminderStatus.SENT,
                "wamid.1",
                null,
                null,
                null
        );
        when(reminderRepository.findByAppointmentKeyAndChannel("A|2026-08-22|10:00", ReminderChannel.DAY_BEFORE))
                .thenReturn(Optional.of(reminder));
        when(appointmentService.markConfirmed("A|2026-08-22|10:00", "573000000001"))
                .thenReturn(new Appointment(
                        "A|2026-08-22|10:00",
                        date,
                        LocalTime.of(10, 0),
                        Consultorio.A,
                        "Julian",
                        "123",
                        "573000000001",
                        "CONFIRMO",
                        10
                ));
        when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handle(new IncomingButtonReply(
                "wamid.btn",
                "573000000001",
                "rc|A|2026-08-22|10:00",
                "Confirmar",
                "1",
                "123"
        ));

        assertTrue(handled);
        verify(appointmentService).markConfirmed("A|2026-08-22|10:00", "573000000001");
        ArgumentCaptor<AppointmentReminder> captor = ArgumentCaptor.forClass(AppointmentReminder.class);
        verify(reminderRepository).save(captor.capture());
        assertEquals(ReminderStatus.CONFIRMED, captor.getValue().status());
        verify(whatsappMessageClient).sendTextMessage(eq("573000000001"), any());
    }

    @Test
    void shouldCancelAppointmentFromButton() {
        LocalDate date = LocalDate.of(2026, 8, 22);
        AppointmentReminder reminder = new AppointmentReminder(
                1L,
                "A|2026-08-22|10:00",
                ReminderChannel.DAY_BEFORE,
                "573000000001",
                "Julian",
                "123",
                date,
                LocalTime.of(10, 0),
                ReminderStatus.SENT,
                "wamid.1",
                null,
                null,
                null
        );
        Appointment cancelled = new Appointment(
                reminder.appointmentKey(),
                date,
                LocalTime.of(10, 0),
                Consultorio.A,
                "Julian",
                "123",
                "573000000001",
                null,
                10
        );

        when(reminderRepository.findByAppointmentKeyAndChannel("A|2026-08-22|10:00", ReminderChannel.DAY_BEFORE))
                .thenReturn(Optional.of(reminder));
        when(appointmentService.cancelByPhone("A|2026-08-22|10:00", "573000000001")).thenReturn(cancelled);
        when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handle(new IncomingButtonReply(
                "wamid.btn",
                "573000000001",
                "rx|A|2026-08-22|10:00",
                "Cancelar",
                "1",
                "123"
        ));

        assertTrue(handled);
        ArgumentCaptor<AppointmentReminder> captor = ArgumentCaptor.forClass(AppointmentReminder.class);
        verify(reminderRepository).save(captor.capture());
        assertEquals(ReminderStatus.CANCELLED, captor.getValue().status());
    }
}
