package com.ocnilentes.mediagenda.modules.whatsapp.inbound.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaWebhookPayload(
        String object,
        List<Entry> entry
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            String id,
            List<Change> changes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(
            String field,
            Value value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            @JsonProperty("messaging_product") String messagingProduct,
            Metadata metadata,
            List<Contact> contacts,
            List<Message> messages,
            List<Status> statuses
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            @JsonProperty("display_phone_number") String displayPhoneNumber,
            @JsonProperty("phone_number_id") String phoneNumberId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
            @JsonProperty("wa_id") String waId,
            @JsonProperty("profile") Profile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("from") String from,
            String id,
            String timestamp,
            String type,
            Text text,
            Interactive interactive,
            Button button
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Text(
            String body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Interactive(
            @JsonProperty("button_reply") ButtonReply buttonReply,
            @JsonProperty("list_reply") ListReply listReply
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ButtonReply(
            String id,
            String title
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListReply(
            String id,
            String title,
            String description
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Button(
        String payload,
        String text
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(
            String id,
            String status,
            String timestamp,
            @JsonProperty("recipient_id") String recipientId
    ) {
    }
}
