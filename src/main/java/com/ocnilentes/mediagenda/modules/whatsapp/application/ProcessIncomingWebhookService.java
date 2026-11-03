package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.reminder.application.ReminderResponseService;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingButtonReply;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.IncomingTextMessage;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

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

    public void process(JsonNode root) {
        if (root == null || root.isNull() || root.isMissingNode()) {
            log.debug("Webhook received with empty body");
            return;
        }

        List<IncomingButtonReply> buttonReplies = extractButtonReplies(root);
        for (IncomingButtonReply reply : buttonReplies) {
            log.info(
                    "Incoming WhatsApp button id={} from={} buttonId='{}' title='{}'",
                    reply.messageId(),
                    reply.from(),
                    reply.buttonId(),
                    reply.buttonTitle()
            );
            if (!reminderResponseService.handle(reply)) {
                conversationService.handle(toConversationMessage(reply));
            }
        }

        List<IncomingTextMessage> listReplies = extractListReplies(root);
        for (IncomingTextMessage message : listReplies) {
            log.info(
                    "Incoming WhatsApp list reply id={} from={} text='{}'",
                    message.messageId(),
                    message.from(),
                    message.text()
            );
            conversationService.handle(message);
        }

        List<IncomingTextMessage> messages = extractTextMessages(root);
        for (IncomingTextMessage message : messages) {
            log.info(
                    "Incoming WhatsApp message id={} from={} text='{}'",
                    message.messageId(),
                    message.from(),
                    message.text()
            );
            conversationService.handle(message);
        }

        if (messages.isEmpty() && buttonReplies.isEmpty() && listReplies.isEmpty()) {
            log.info(
                    "Webhook without text/button/list messages statuses={} messageTypes={} contacts={}",
                    statusIds(root),
                    messageTypes(root),
                    contactWaIdsFromRoot(root)
            );
        }
    }

    private List<String> statusIds(JsonNode root) {
        List<String> ids = new ArrayList<>();
        for (JsonNode changeValue : changeValues(root)) {
            for (JsonNode status : iterable(changeValue.path("statuses"))) {
                String id = textOrNull(status.path("id"));
                String statusValue = textOrNull(status.path("status"));
                String recipient = textOrNull(status.path("recipient_id"));
                ids.add(statusValue + ":" + recipient + ":" + id);
            }
        }
        return ids;
    }

    private List<String> messageTypes(JsonNode root) {
        List<String> types = new ArrayList<>();
        for (JsonNode changeValue : changeValues(root)) {
            for (JsonNode message : iterable(changeValue.path("messages"))) {
                types.add(textOrNull(message.path("type")));
            }
        }
        return types;
    }

    private List<String> contactWaIdsFromRoot(JsonNode root) {
        List<String> ids = new ArrayList<>();
        for (JsonNode changeValue : changeValues(root)) {
            ids.addAll(contactWaIds(changeValue));
        }
        return ids;
    }

    private List<IncomingTextMessage> extractTextMessages(JsonNode root) {
        List<IncomingTextMessage> result = new ArrayList<>();

        for (JsonNode changeValue : changeValues(root)) {
            String phoneNumberId = textOrNull(changeValue.path("metadata").path("phone_number_id"));

            for (JsonNode message : iterable(changeValue.path("messages"))) {
                if (!isTextMessage(message)) {
                    continue;
                }

                String sender = resolveSender(message, changeValue);
                if (!StringUtils.hasText(sender)) {
                    log.warn(
                            "Ignoring WhatsApp text message without sender id={} type={} messageJson={} contactWaIds={}",
                            textOrNull(message.path("id")),
                            textOrNull(message.path("type")),
                            message,
                            contactWaIds(changeValue)
                    );
                    continue;
                }

                result.add(new IncomingTextMessage(
                        textOrNull(message.path("id")),
                        WhatsappPhoneNormalizer.normalize(sender),
                        textOrNull(message.path("text").path("body")),
                        textOrNull(message.path("timestamp")),
                        phoneNumberId
                ));
            }
        }

        return result;
    }

    private List<IncomingButtonReply> extractButtonReplies(JsonNode root) {
        List<IncomingButtonReply> result = new ArrayList<>();

        for (JsonNode changeValue : changeValues(root)) {
            String phoneNumberId = textOrNull(changeValue.path("metadata").path("phone_number_id"));

            for (JsonNode message : iterable(changeValue.path("messages"))) {
                IncomingButtonReply reply = toButtonReply(message, changeValue, phoneNumberId);
                if (reply != null) {
                    result.add(reply);
                } else if (!isTextMessage(message) && !isListReply(message)) {
                    log.debug(
                            "Ignoring unsupported WhatsApp message type={} id={}",
                            textOrNull(message.path("type")),
                            textOrNull(message.path("id"))
                    );
                }
            }
        }

        return result;
    }

    private IncomingButtonReply toButtonReply(JsonNode message, JsonNode changeValue, String phoneNumberId) {
        String type = textOrNull(message.path("type"));
        if (!StringUtils.hasText(type)) {
            return null;
        }

        String sender = resolveSender(message, changeValue);
        if (!StringUtils.hasText(sender)) {
            return null;
        }

        String normalizedSender = WhatsappPhoneNormalizer.normalize(sender);

        if ("interactive".equalsIgnoreCase(type) && message.path("interactive").has("button_reply")) {
            JsonNode buttonReply = message.path("interactive").path("button_reply");
            return new IncomingButtonReply(
                    textOrNull(message.path("id")),
                    normalizedSender,
                    textOrNull(buttonReply.path("id")),
                    textOrNull(buttonReply.path("title")),
                    textOrNull(message.path("timestamp")),
                    phoneNumberId
            );
        }

        if ("button".equalsIgnoreCase(type) && message.has("button")) {
            JsonNode button = message.path("button");
            return new IncomingButtonReply(
                    textOrNull(message.path("id")),
                    normalizedSender,
                    textOrNull(button.path("payload")),
                    textOrNull(button.path("text")),
                    textOrNull(message.path("timestamp")),
                    phoneNumberId
            );
        }

        return null;
    }

    private List<IncomingTextMessage> extractListReplies(JsonNode root) {
        List<IncomingTextMessage> result = new ArrayList<>();

        for (JsonNode changeValue : changeValues(root)) {
            String phoneNumberId = textOrNull(changeValue.path("metadata").path("phone_number_id"));

            for (JsonNode message : iterable(changeValue.path("messages"))) {
                if (!isListReply(message)) {
                    continue;
                }

                String sender = resolveSender(message, changeValue);
                if (!StringUtils.hasText(sender)) {
                    log.warn(
                            "Ignoring WhatsApp list reply without sender id={} messageJson={}",
                            textOrNull(message.path("id")),
                            message
                    );
                    continue;
                }

                JsonNode listReply = message.path("interactive").path("list_reply");
                String option = textOrNull(listReply.path("id"));
                if (!StringUtils.hasText(option)) {
                    option = textOrNull(listReply.path("title"));
                }
                result.add(new IncomingTextMessage(
                        textOrNull(message.path("id")),
                        WhatsappPhoneNormalizer.normalize(sender),
                        option,
                        textOrNull(message.path("timestamp")),
                        phoneNumberId
                ));
            }
        }

        return result;
    }

    private IncomingTextMessage toConversationMessage(IncomingButtonReply reply) {
        String text = StringUtils.hasText(reply.buttonId()) ? reply.buttonId() : reply.buttonTitle();
        return new IncomingTextMessage(
                reply.messageId(),
                reply.from(),
                text,
                reply.timestamp(),
                reply.phoneNumberId()
        );
    }

    private String resolveSender(JsonNode message, JsonNode changeValue) {
        String from = textOrNull(message.path("from"));
        if (StringUtils.hasText(from)) {
            return from;
        }

        for (JsonNode contact : iterable(changeValue.path("contacts"))) {
            String waId = textOrNull(contact.path("wa_id"));
            if (StringUtils.hasText(waId)) {
                return waId;
            }
        }

        return null;
    }

    private List<String> contactWaIds(JsonNode changeValue) {
        List<String> ids = new ArrayList<>();
        for (JsonNode contact : iterable(changeValue.path("contacts"))) {
            String waId = textOrNull(contact.path("wa_id"));
            if (StringUtils.hasText(waId)) {
                ids.add(waId);
            }
        }
        return ids;
    }

    private List<JsonNode> changeValues(JsonNode root) {
        List<JsonNode> values = new ArrayList<>();
        for (JsonNode entry : iterable(root.path("entry"))) {
            for (JsonNode change : iterable(entry.path("changes"))) {
                JsonNode value = change.path("value");
                if (!value.isMissingNode() && !value.isNull()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private boolean isTextMessage(JsonNode message) {
        return "text".equalsIgnoreCase(textOrNull(message.path("type")))
                && StringUtils.hasText(textOrNull(message.path("text").path("body")));
    }

    private boolean isListReply(JsonNode message) {
        return "interactive".equalsIgnoreCase(textOrNull(message.path("type")))
                && message.path("interactive").has("list_reply");
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isValueNode()) {
            return null;
        }
        String text = node.asString("");
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private Iterable<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return List.of();
        }
        List<JsonNode> values = new ArrayList<>();
        node.forEach(values::add);
        return values;
    }
}
