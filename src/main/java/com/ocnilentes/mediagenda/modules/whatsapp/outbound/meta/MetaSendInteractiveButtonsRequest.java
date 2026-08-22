package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ReplyButton;
import java.util.List;

record MetaSendInteractiveButtonsRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("recipient_type") String recipientType,
        String to,
        String type,
        Interactive interactive
) {

    static MetaSendInteractiveButtonsRequest of(String to, String body, List<ReplyButton> buttons) {
        List<Button> metaButtons = buttons.stream()
                .map(button -> new Button(
                        "reply",
                        new Reply(button.id(), button.title())
                ))
                .toList();
        return new MetaSendInteractiveButtonsRequest(
                "whatsapp",
                "individual",
                to,
                "interactive",
                new Interactive(
                        "button",
                        new Body(body),
                        new Action(metaButtons)
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

    record Action(List<Button> buttons) {
    }

    record Button(
            String type,
            Reply reply
    ) {
    }

    record Reply(
            String id,
            String title
    ) {
    }
}
