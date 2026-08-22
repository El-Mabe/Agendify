package com.ocnilentes.mediagenda.modules.whatsapp.domain.model;

public record IncomingTextMessage(
        String messageId,
        String from,
        String text,
        String timestamp,
        String phoneNumberId
) {
}
