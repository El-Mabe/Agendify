package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.exception.WhatsappApiException;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.InteractiveListRow;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ReplyButton;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import com.ocnilentes.mediagenda.shared.config.WhatsappProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MetaWhatsappMessageClient implements WhatsappMessageClient {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsappMessageClient.class);

    private final RestClient whatsappRestClient;
    private final WhatsappProperties properties;

    public MetaWhatsappMessageClient(
            @Qualifier("whatsappRestClient") RestClient whatsappRestClient,
            WhatsappProperties properties
    ) {
        this.whatsappRestClient = whatsappRestClient;
        this.properties = properties;
    }

    @Override
    public String sendTextMessage(String to, String body) {
        return send(to, MetaSendTextMessageRequest.of(to, body));
    }

    @Override
    public String sendInteractiveButtons(String to, String body, List<ReplyButton> buttons) {
        if (buttons == null || buttons.isEmpty() || buttons.size() > 3) {
            throw new IllegalArgumentException("Interactive messages require 1 to 3 reply buttons");
        }
        return send(to, MetaSendInteractiveButtonsRequest.of(to, body, buttons));
    }

    @Override
    public String sendInteractiveList(
            String to,
            String body,
            String buttonLabel,
            String sectionTitle,
            List<InteractiveListRow> rows
    ) {
        if (rows == null || rows.isEmpty() || rows.size() > 10) {
            throw new IllegalArgumentException("Interactive lists require 1 to 10 rows");
        }
        if (!StringUtils.hasText(buttonLabel) || buttonLabel.length() > 20) {
            throw new IllegalArgumentException("Interactive list button label must be 1 to 20 characters");
        }
        return send(to, MetaSendInteractiveListRequest.of(to, body, buttonLabel, sectionTitle, rows));
    }

    @Override
    public String sendTemplate(String to, String templateName, String languageCode, List<String> bodyParameters) {
        return sendTemplate(to, templateName, languageCode, bodyParameters, List.of());
    }

    @Override
    public String sendTemplate(
            String to,
            String templateName,
            String languageCode,
            List<String> bodyParameters,
            List<ReplyButton> quickReplyButtons
    ) {
        requireConfigured(templateName, "Template name is required");
        requireConfigured(languageCode, "Template language is required");
        return send(
                to,
                MetaSendTemplateMessageRequest.of(
                        to,
                        templateName,
                        languageCode,
                        bodyParameters == null ? List.of() : bodyParameters,
                        quickReplyButtons == null ? List.of() : quickReplyButtons
                )
        );
    }

    private String send(String to, Object body) {
        requireConfigured(properties.phoneNumberId(), "whatsapp.phone-number-id is not configured");
        requireConfigured(properties.token(), "whatsapp.token is not configured");

        try {
            MetaSendMessageResponse response = whatsappRestClient.post()
                    .uri("/{phoneNumberId}/messages", properties.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MetaSendMessageResponse.class);

            return extractMessageId(response);
        } catch (RestClientResponseException exception) {
            int statusCode = exception.getStatusCode().value();
            String responseBody = exception.getResponseBodyAsString();
            log.error(
                    "Meta Cloud API rejected message to={} phoneNumberId={} status={} body={}",
                    to,
                    properties.phoneNumberId(),
                    statusCode,
                    responseBody
            );
            throw new WhatsappApiException(
                    "Meta Cloud API rejected the message request",
                    statusCode,
                    responseBody
            );
        } catch (WhatsappApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Failed to send WhatsApp message via Meta Cloud API to={}", to, exception);
            throw new WhatsappApiException("Failed to send WhatsApp message via Meta Cloud API", exception);
        }
    }

    private String extractMessageId(MetaSendMessageResponse response) {
        if (response == null || response.messages() == null || response.messages().isEmpty()) {
            throw new WhatsappApiException("Meta Cloud API returned an empty message response", 502, null);
        }

        String messageId = response.messages().getFirst().id();
        if (!StringUtils.hasText(messageId)) {
            throw new WhatsappApiException("Meta Cloud API returned a message without id", 502, null);
        }

        return messageId;
    }

    private void requireConfigured(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
