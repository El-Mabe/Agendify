package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.port.WhatsappMessageClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SendTestMessageService {

    private final WhatsappMessageClient whatsappMessageClient;

    public SendTestMessageService(WhatsappMessageClient whatsappMessageClient) {
        this.whatsappMessageClient = whatsappMessageClient;
    }

    public SendTestMessageResult execute(SendTestMessageCommand command) {
        requireText(command.to(), "Recipient phone number is required");
        requireText(command.message(), "Message body is required");

        String messageId = whatsappMessageClient.sendTextMessage(command.to().trim(), command.message().trim());
        return new SendTestMessageResult(messageId, command.to().trim());
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
