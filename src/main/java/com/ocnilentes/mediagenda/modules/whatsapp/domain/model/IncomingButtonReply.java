package com.ocnilentes.mediagenda.modules.whatsapp.domain.model;

public record IncomingButtonReply(
        String messageId,
        String from,
        String buttonId,
        String buttonTitle,
        String timestamp,
        String phoneNumberId
) {
}
