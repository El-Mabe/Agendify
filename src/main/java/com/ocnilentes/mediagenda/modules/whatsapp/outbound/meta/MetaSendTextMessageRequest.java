package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

record MetaSendTextMessageRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("recipient_type") String recipientType,
        String to,
        String type,
        TextPayload text
) {

    static MetaSendTextMessageRequest of(String to, String body) {
        return new MetaSendTextMessageRequest(
                "whatsapp",
                "individual",
                to,
                "text",
                new TextPayload(false, body)
        );
    }

    record TextPayload(
            @JsonProperty("preview_url") boolean previewUrl,
            String body
    ) {
    }
}
