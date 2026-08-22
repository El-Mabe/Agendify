package com.ocnilentes.mediagenda.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reminder")
public record ReminderProperties(
        boolean enabled,
        int dayBeforeHour,
        int windowEndHour,
        String sendMode,
        String templateName,
        String templateLanguage,
        String manualRunToken
) {
    public ReminderProperties {
        if (dayBeforeHour < 0 || dayBeforeHour > 23) {
            dayBeforeHour = 18;
        }
        if (windowEndHour < 0 || windowEndHour > 23) {
            windowEndHour = 22;
        }
        if (sendMode == null || sendMode.isBlank()) {
            sendMode = "INTERACTIVE";
        }
        if (templateLanguage == null || templateLanguage.isBlank()) {
            templateLanguage = "es_CO";
        }
    }

    public boolean isTemplateMode() {
        return "TEMPLATE".equalsIgnoreCase(sendMode);
    }
}
