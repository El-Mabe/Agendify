package com.ocnilentes.mediagenda.modules.whatsapp.application;

import org.springframework.util.StringUtils;

/**
 * Normaliza números colombianos para la Cloud API (E.164 sin +).
 */
final class WhatsappPhoneNormalizer {

    private WhatsappPhoneNormalizer() {
    }

    static String normalize(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }

        String digits = phone.trim().replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }

        // Celular Colombia local: 3XXXXXXXXX (10 dígitos) → 57XXXXXXXXXX
        if (digits.length() == 10 && digits.startsWith("3")) {
            return "57" + digits;
        }

        return digits;
    }
}
