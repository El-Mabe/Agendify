package com.ocnilentes.mediagenda.modules.whatsapp.application;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationSession;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ConversationSessionStore {

    static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    public boolean discardIfIdle(String phone) {
        ConversationSession session = sessions.get(phone);
        if (session == null) {
            return false;
        }
        if (session.isIdleExpired(IDLE_TIMEOUT)) {
            sessions.remove(phone);
            return true;
        }
        return false;
    }

    public ConversationSession getOrCreate(String phone) {
        ConversationSession session = sessions.computeIfAbsent(phone, ConversationSession::new);
        session.touch();
        return session;
    }
}
