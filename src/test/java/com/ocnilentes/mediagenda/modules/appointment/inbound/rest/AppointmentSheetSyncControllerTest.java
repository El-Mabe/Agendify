package com.ocnilentes.mediagenda.modules.appointment.inbound.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.ocnilentes.mediagenda.modules.appointment.application.AppointmentSheetReconciliationService;
import com.ocnilentes.mediagenda.modules.appointment.application.ReceptionSheetSyncService;
import com.ocnilentes.mediagenda.shared.config.AppointmentSheetSyncProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AppointmentSheetSyncControllerTest {

    private AppointmentSheetSyncController controller;

    @BeforeEach
    void setUp() {
        AppointmentSheetSyncProperties properties = new AppointmentSheetSyncProperties(
                true,
                "expected-token",
                true,
                1,
                3,
                true,
                1
        );
        controller = new AppointmentSheetSyncController(
                mock(ReceptionSheetSyncService.class),
                mock(AppointmentSheetReconciliationService.class),
                properties
        );
    }

    @Test
    void rejectsInvalidWebhookToken() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.synchronizeRow("wrong-token", null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
