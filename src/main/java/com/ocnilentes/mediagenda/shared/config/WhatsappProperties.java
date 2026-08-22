package com.ocnilentes.mediagenda.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "whatsapp")
public record WhatsappProperties(
        String apiUrl,
        String version,
        String token,
        String phoneNumberId,
        String verifyToken
) {
}
