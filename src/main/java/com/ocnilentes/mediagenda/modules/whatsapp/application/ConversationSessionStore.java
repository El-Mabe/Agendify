package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ConversationSessionStore {

    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    public ConversationSession getOrCreate(String phone) {
        return sessions.computeIfAbsent(phone, ConversationSession::new);
    }
}
