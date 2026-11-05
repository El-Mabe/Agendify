package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.InteractiveListRow;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record MetaSendInteractiveListRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("recipient_type") String recipientType,
        String to,
        String recipient,
        String type,
        Interactive interactive
) {

    static MetaSendInteractiveListRequest of(
            String to,
            String body,
            String buttonLabel,
            String sectionTitle,
            List<InteractiveListRow> rows
    ) {
        List<Row> metaRows = rows.stream()
                .map(row -> new Row(row.id(), row.title(), row.description()))
                .toList();
        MetaRecipientAddress address = MetaRecipientAddress.of(to);
        return new MetaSendInteractiveListRequest(
                "whatsapp",
                "individual",
                address.to(),
                address.recipient(),
                "interactive",
                new Interactive(
                        "list",
                        new Body(body),
                        new Action(
                                buttonLabel,
                                List.of(new Section(sectionTitle, metaRows))
                        )
                )
        );
    }

    record Interactive(
            String type,
            Body body,
            Action action
    ) {
    }

    record Body(String text) {
    }

    record Action(
            String button,
            List<Section> sections
    ) {
    }

    record Section(
            String title,
            List<Row> rows
    ) {
    }

    record Row(
            String id,
            String title,
            String description
    ) {
    }
}
