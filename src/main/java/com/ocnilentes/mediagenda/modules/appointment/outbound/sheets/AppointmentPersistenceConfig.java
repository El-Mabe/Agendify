package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleAccessTokenProvider;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsApiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class AppointmentPersistenceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "google.sheets", name = "enabled", havingValue = "true")
    GoogleAccessTokenProvider googleAccessTokenProvider(GoogleSheetsProperties properties) {
        if (!properties.hasOperationalSpreadsheet() || !hasCredentials(properties)) {
            throw new IllegalStateException(
                    "When google.sheets.enabled=true configure credentials and either "
                            + "google.sheets.spreadsheet-id (legacy) or both "
                            + "spreadsheet-id-a and spreadsheet-id-b"
            );
        }
        return new GoogleAccessTokenProvider(properties);
    }

    private boolean hasCredentials(GoogleSheetsProperties properties) {
        return StringUtils.hasText(properties.credentialsJson()) || StringUtils.hasText(properties.credentialsPath());
    }

    @Bean
    @ConditionalOnProperty(prefix = "google.sheets", name = "enabled", havingValue = "true")
    GoogleSheetsApiClient googleSheetsApiClient(GoogleAccessTokenProvider tokenProvider) {
        return new GoogleSheetsApiClient(RestClient.builder(), tokenProvider);
    }
}
