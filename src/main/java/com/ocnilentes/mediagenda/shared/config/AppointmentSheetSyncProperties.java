package com.ocnilentes.mediagenda.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "appointments.sheet-sync")
public record AppointmentSheetSyncProperties(
        boolean enabled,
        String webhookToken,
        boolean bootstrapEnabled,
        int bootstrapMonthsBack,
        int bootstrapMonthsForward,
        boolean reconciliationEnabled,
        int reconciliationMonthsForward
) {
}
