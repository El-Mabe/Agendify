package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record MetaSendTextMessageRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("recipient_type") String recipientType,
        String to,
        String recipient,
        String type,
        TextPayload text
) {

    static MetaSendTextMessageRequest of(String to, String body) {
        MetaRecipientAddress address = MetaRecipientAddress.of(to);
        return new MetaSendTextMessageRequest(
                "whatsapp",
                "individual",
                address.to(),
                address.recipient(),
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
