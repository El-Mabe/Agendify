package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.reminder.application.ReminderResponseService;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingButtonReply;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingTextMessage;
import com.ocnilentes.mediagenda.modules.whatsapp.inbound.webhook.MetaWebhookPayload;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ProcessIncomingWebhookService {

    private static final Logger log = LoggerFactory.getLogger(ProcessIncomingWebhookService.class);

    private final ConversationService conversationService;
    private final ReminderResponseService reminderResponseService;

    public ProcessIncomingWebhookService(
            ConversationService conversationService,
            ReminderResponseService reminderResponseService
    ) {
        this.conversationService = conversationService;
        this.reminderResponseService = reminderResponseService;
    }

    public List<IncomingTextMessage> process(MetaWebhookPayload payload) {
        List<IncomingButtonReply> buttonReplies = extractButtonReplies(payload);
        for (IncomingButtonReply reply : buttonReplies) {
            log.info(
                    "Incoming WhatsApp button id={} from={} buttonId='{}' title='{}'",
                    reply.messageId(),
                    reply.from(),
                    reply.buttonId(),
                    reply.buttonTitle()
            );
            reminderResponseService.handle(reply);
        }

        List<IncomingTextMessage> messages = extractTextMessages(payload);
        for (IncomingTextMessage message : messages) {
            log.info(
                    "Incoming WhatsApp message id={} from={} text='{}'",
                    message.messageId(),
                    message.from(),
                    message.text()
            );
            conversationService.handle(message);
        }

        if (messages.isEmpty() && buttonReplies.isEmpty()) {
            log.debug("Webhook received without text/button messages (status update or unsupported type)");
        }

        return messages;
    }

    private List<IncomingTextMessage> extractTextMessages(MetaWebhookPayload payload) {
        List<IncomingTextMessage> result = new ArrayList<>();

        if (payload == null || CollectionUtils.isEmpty(payload.entry())) {
            return result;
        }

        for (MetaWebhookPayload.Entry entry : payload.entry()) {
            if (CollectionUtils.isEmpty(entry.changes())) {
                continue;
            }

            for (MetaWebhookPayload.Change change : entry.changes()) {
                MetaWebhookPayload.Value value = change.value();
                if (value == null || CollectionUtils.isEmpty(value.messages())) {
                    continue;
                }

                String phoneNumberId = value.metadata() != null ? value.metadata().phoneNumberId() : null;

                for (MetaWebhookPayload.Message message : value.messages()) {
                    if (!isTextMessage(message)) {
                        continue;
                    }

                    result.add(new IncomingTextMessage(
                            message.id(),
                            message.from(),
                            message.text().body(),
                            message.timestamp(),
                            phoneNumberId
                    ));
                }
            }
        }

        return result;
    }

    private List<IncomingButtonReply> extractButtonReplies(MetaWebhookPayload payload) {
        List<IncomingButtonReply> result = new ArrayList<>();

        if (payload == null || CollectionUtils.isEmpty(payload.entry())) {
            return result;
        }

        for (MetaWebhookPayload.Entry entry : payload.entry()) {
            if (CollectionUtils.isEmpty(entry.changes())) {
                continue;
            }

            for (MetaWebhookPayload.Change change : entry.changes()) {
                MetaWebhookPayload.Value value = change.value();
                if (value == null || CollectionUtils.isEmpty(value.messages())) {
                    continue;
                }

                String phoneNumberId = value.metadata() != null ? value.metadata().phoneNumberId() : null;

                for (MetaWebhookPayload.Message message : value.messages()) {
                    IncomingButtonReply reply = toButtonReply(message, phoneNumberId);
                    if (reply != null) {
                        result.add(reply);
                    } else if (!isTextMessage(message)) {
                        log.debug(
                                "Ignoring unsupported WhatsApp message type={} id={}",
                                message.type(),
                                message.id()
                        );
                    }
                }
            }
        }

        return result;
    }

    private IncomingButtonReply toButtonReply(MetaWebhookPayload.Message message, String phoneNumberId) {
        if (message == null || !StringUtils.hasText(message.type())) {
            return null;
        }

        if ("interactive".equalsIgnoreCase(message.type())
                && message.interactive() != null
                && message.interactive().buttonReply() != null) {
            MetaWebhookPayload.ButtonReply buttonReply = message.interactive().buttonReply();
            return new IncomingButtonReply(
                    message.id(),
                    message.from(),
                    buttonReply.id(),
                    buttonReply.title(),
                    message.timestamp(),
                    phoneNumberId
            );
        }

        if ("button".equalsIgnoreCase(message.type()) && message.button() != null) {
            return new IncomingButtonReply(
                    message.id(),
                    message.from(),
                    message.button().payload(),
                    message.button().text(),
                    message.timestamp(),
                    phoneNumberId
            );
        }

        return null;
    }

    private boolean isTextMessage(MetaWebhookPayload.Message message) {
        return message != null
                && "text".equalsIgnoreCase(message.type())
                && message.text() != null
                && StringUtils.hasText(message.text().body());
    }
}
