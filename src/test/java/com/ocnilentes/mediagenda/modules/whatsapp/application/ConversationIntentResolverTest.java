package com.ocnilentes.mediagenda.modules.whatsapp.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ConversationIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationIntentResolverTest {

    private ConversationIntentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ConversationIntentResolver();
    }

    @Test
    void shouldResolveScheduleFromNumber() {
        assertEquals(ConversationIntent.SCHEDULE, resolver.resolve("1"));
    }

    @Test
    void shouldResolveCancelFromKeyword() {
        assertEquals(ConversationIntent.CANCEL, resolver.resolve("Cancelar"));
    }

    @Test
    void shouldResolveInformationFromNumber() {
        assertEquals(ConversationIntent.INFORMATION, resolver.resolve("3"));
    }

    @Test
    void shouldShowMenuForGreeting() {
        assertEquals(ConversationIntent.SHOW_MENU, resolver.resolve("Hola"));
    }
}
