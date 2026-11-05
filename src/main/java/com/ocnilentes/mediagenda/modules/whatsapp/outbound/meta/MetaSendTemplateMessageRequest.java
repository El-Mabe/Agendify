package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ReplyButton;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record MetaSendTemplateMessageRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("recipient_type") String recipientType,
        String to,
        String recipient,
        String type,
        Template template
) {

    static MetaSendTemplateMessageRequest of(
            String to,
            String templateName,
            String languageCode,
            List<String> bodyParameters,
            List<ReplyButton> quickReplyButtons
    ) {
        List<Component> components = new ArrayList<>();
        List<Parameter> bodyParams = bodyParameters.stream()
                .map(Parameter::text)
                .toList();
        components.add(new Component("body", null, null, bodyParams));

        if (quickReplyButtons != null) {
            for (int index = 0; index < quickReplyButtons.size(); index++) {
                ReplyButton button = quickReplyButtons.get(index);
                components.add(new Component(
                        "button",
                        "quick_reply",
                        String.valueOf(index),
                        List.of(Parameter.payload(button.id()))
                ));
            }
        }

        MetaRecipientAddress address = MetaRecipientAddress.of(to);
        return new MetaSendTemplateMessageRequest(
                "whatsapp",
                "individual",
                address.to(),
                address.recipient(),
                "template",
                new Template(
                        templateName,
                        new Language(languageCode),
                        components
                )
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Template(
            String name,
            Language language,
            List<Component> components
    ) {
    }

    record Language(String code) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Component(
            String type,
            @JsonProperty("sub_type") String subType,
            String index,
            List<Parameter> parameters
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Parameter(
            String type,
            String text,
            String payload
    ) {
        static Parameter text(String value) {
            return new Parameter("text", value, null);
        }

        static Parameter payload(String value) {
            return new Parameter("payload", null, value);
        }
    }
}
