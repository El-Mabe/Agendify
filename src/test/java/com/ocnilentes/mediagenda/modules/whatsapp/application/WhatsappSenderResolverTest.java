package com.ocnilentes.mediagenda.modules.whatsapp.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WhatsappSenderResolverTest {

    @Test
    void prefersPhoneWaIdWhenFromIsLid() {
        assertThat(WhatsappSenderResolver.resolve("165043027399123@lid", "573043370408"))
                .isEqualTo("573043370408");
    }

    @Test
    void usesFromWhenItIsThePhoneAndWaIdIsLid() {
        assertThat(WhatsappSenderResolver.resolve("573043370408", "165043027399123@lid"))
                .isEqualTo("573043370408");
    }

    @Test
    void keepsColombianLocalFromWhenWaIdMissing() {
        assertThat(WhatsappSenderResolver.resolve("3183108750", null))
                .isEqualTo("573183108750");
    }
}
