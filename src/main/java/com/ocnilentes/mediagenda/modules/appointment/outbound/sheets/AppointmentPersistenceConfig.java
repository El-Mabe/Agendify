package com.ocnilentes.mediagenda.modules.appointment.outbound.sheets;

import com.ocnilentes.mediagenda.modules.appointment.domain.port.AppointmentRepository;
import com.ocnilentes.mediagenda.modules.appointment.outbound.memory.InMemoryAppointmentRepository;
import com.ocnilentes.mediagenda.shared.config.GoogleSheetsProperties;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleAccessTokenProvider;
import com.ocnilentes.mediagenda.shared.outbound.google.GoogleSheetsApiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
        if (!StringUtils.hasText(properties.spreadsheetId()) || !hasCredentials(properties)) {
            throw new IllegalStateException(
                    "google.sheets.spreadsheet-id and credentials (credentials-json or credentials-path) "
                            + "are required when google.sheets.enabled=true"
            );
        }
        return new GoogleAccessTokenProvider(properties);
    }

    private boolean hasCredentials(GoogleSheetsProperties properties) {
        return StringUtils.hasText(properties.credentialsJson()) || StringUtils.hasText(properties.credentialsPath());
    }

    @Bean
    @ConditionalOnProperty(prefix = "google.sheets", name = "enabled", havingValue = "true")
    GoogleSheetsApiClient googleSheetsApiClient(
            GoogleAccessTokenProvider tokenProvider,
            GoogleSheetsProperties properties
    ) {
        return new GoogleSheetsApiClient(RestClient.builder(), tokenProvider, properties.spreadsheetId());
    }

    @Bean
    @ConditionalOnProperty(prefix = "google.sheets", name = "enabled", havingValue = "true")
    AppointmentRepository googleSheetsAppointmentRepository(
            GoogleSheetsApiClient googleSheetsApiClient,
            GoogleSheetsProperties properties
    ) {
        return new GoogleSheetsAppointmentRepository(googleSheetsApiClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean(AppointmentRepository.class)
    AppointmentRepository inMemoryAppointmentRepository() {
        return new InMemoryAppointmentRepository();
    }
}
