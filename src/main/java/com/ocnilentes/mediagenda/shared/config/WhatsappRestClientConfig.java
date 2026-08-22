package com.ocnilentes.mediagenda.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class WhatsappRestClientConfig {

    @Bean
    RestClient whatsappRestClient(WhatsappProperties properties) {
        return RestClient.builder()
                .baseUrl("%s/%s".formatted(properties.apiUrl(), properties.version()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
