package com.ocnilentes.mediagenda.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "attendance-sync")
public record AttendanceSyncProperties(
        boolean enabled,
        int windowStartHour,
        int windowEndHour,
        String manualRunToken
) {
    public AttendanceSyncProperties {
        if (windowStartHour < 0 || windowStartHour > 23) {
            windowStartHour = 20;
        }
        if (windowEndHour < 0 || windowEndHour > 23) {
            windowEndHour = 22;
        }
    }
}
