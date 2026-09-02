package com.ocnilentes.mediagenda.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.sheets")
public record GoogleSheetsProperties(
        boolean enabled,
        String spreadsheetId,
        String credentialsPath,
        String credentialsJson,
        String appointmentsSheet,
        String holidaysSheet,
        String informationSheet
) {
}
