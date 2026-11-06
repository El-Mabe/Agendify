package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationIntent;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ConversationIntentResolver {

    public ConversationIntent resolve(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return ConversationIntent.SHOW_MENU;
        }

        String text = normalize(rawText);

        if (isSchedule(text)) {
            return ConversationIntent.SCHEDULE;
        }
        if (isCancel(text)) {
            return ConversationIntent.CANCEL;
        }
        if (isInformation(text)) {
            return ConversationIntent.INFORMATION;
        }

        return ConversationIntent.SHOW_MENU;
    }

    private boolean isSchedule(String text) {
        return text.equals("1")
                || text.equals("agendar")
                || text.equals("cita")
                || text.equals("agendar cita");
    }

    private boolean isCancel(String text) {
        return text.equals("2")
                || text.equals("cancelar")
                || text.equals("cancelar cita");
    }

    private boolean isInformation(String text) {
        return text.equals("3")
                || text.equals("informacion")
                || text.equals("información")
                || text.equals("otra informacion")
                || text.equals("otra información")
                || text.equals("info");
    }

    private String normalize(String rawText) {
        return rawText
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('\u00a0', ' ');
    }
}
