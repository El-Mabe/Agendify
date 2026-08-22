package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record MetaSendTemplateMessageRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("recipient_type") String recipientType,
        String to,
        String type,
        Template template
) {

    static MetaSendTemplateMessageRequest of(
            String to,
            String templateName,
            String languageCode,
            List<String> bodyParameters
    ) {
        List<Parameter> parameters = bodyParameters.stream()
                .map(Parameter::text)
                .toList();
        return new MetaSendTemplateMessageRequest(
                "whatsapp",
                "individual",
                to,
                "template",
                new Template(
                        templateName,
                        new Language(languageCode),
                        List.of(new Component("body", parameters))
                )
        );
    }

    record Template(
            String name,
            Language language,
            List<Component> components
    ) {
    }

    record Language(String code) {
    }

    record Component(
            String type,
            List<Parameter> parameters
    ) {
    }

    record Parameter(
            String type,
            String text
    ) {
        static Parameter text(String value) {
            return new Parameter("text", value);
        }
    }
}
