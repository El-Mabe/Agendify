package com.ocnilentes.mediagenda.modules.whatsapp.inbound.rest;

import jakarta.validation.constraints.NotBlank;

public record SendTestMessageRequest(
        @NotBlank(message = "must not be blank")
        String to,

        String message
) {
}
