package com.ocnilentes.mediagenda.modules.whatsapp.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WhatsappPhoneNormalizerTest {

    @Test
    void normalize_addsColombiaCountryCodeToLocalMobile() {
        assertThat(WhatsappPhoneNormalizer.normalize("3183108750")).isEqualTo("573183108750");
    }

    @Test
    void normalize_keepsAlreadyInternationalNumber() {
        assertThat(WhatsappPhoneNormalizer.normalize("573043370408")).isEqualTo("573043370408");
    }

    @Test
    void normalize_stripsNonDigits() {
        assertThat(WhatsappPhoneNormalizer.normalize("+57 318 310 8750")).isEqualTo("573183108750");
    }

    @Test
    void normalize_doesNotPrefixCountryCodeOnLid() {
        assertThat(WhatsappPhoneNormalizer.normalize("165043027399123@lid"))
                .isEqualTo("165043027399123");
        assertThat(WhatsappPhoneNormalizer.isLikelyPhone("165043027399123@lid")).isFalse();
    }
}
