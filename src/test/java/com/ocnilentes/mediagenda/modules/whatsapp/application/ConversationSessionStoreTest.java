package com.ocnilentes.mediagenda.modules.whatsapp.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationSession;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConversationSessionStoreTest {

    @Test
    void shouldKeepActiveSession() {
        ConversationSessionStore store = new ConversationSessionStore();
        store.getOrCreate("573001112233");

        assertFalse(store.discardIfIdle("573001112233"));
    }

    @Test
    void shouldDiscardIdleSessionAfterTimeout() {
        ConversationSessionStore store = new ConversationSessionStore();
        ConversationSession session = store.getOrCreate("573001112233");
        session.setLastActivity(Instant.now().minus(ConversationSessionStore.IDLE_TIMEOUT).minusSeconds(1));

        assertTrue(store.discardIfIdle("573001112233"));
        assertFalse(store.discardIfIdle("573001112233"));
    }
}
