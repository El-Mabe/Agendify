package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record MetaSendMessageResponse(
        @JsonProperty("messaging_product") String messagingProduct,
        List<Message> messages,
        List<Contact> contacts
) {

    record Message(String id) {
    }

    record Contact(
            String input,
            @JsonProperty("wa_id") String waId
    ) {
    }
}
