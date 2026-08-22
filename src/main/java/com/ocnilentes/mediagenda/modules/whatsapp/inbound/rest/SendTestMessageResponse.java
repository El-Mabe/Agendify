package com.ocnilentes.mediagenda.modules.whatsapp.inbound.rest;

public record SendTestMessageResponse(
        String status,
        String messageId,
        String to
) {
}
