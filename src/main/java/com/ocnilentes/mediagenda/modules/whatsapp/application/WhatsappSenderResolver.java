package com.ocnilentes.mediagenda.modules.whatsapp.application;

import org.springframework.util.StringUtils;

/**
 * Elige el destinatario de la Cloud API. Meta a veces manda un LID en {@code messages.from}
 * y el teléfono real en {@code contacts.wa_id}; hay que responder al teléfono.
 */
final class WhatsappSenderResolver {

    private WhatsappSenderResolver() {
    }

    static String resolve(String from, String waId) {
        if (WhatsappPhoneNormalizer.isLikelyPhone(waId)) {
            return WhatsappPhoneNormalizer.normalize(waId);
        }
        if (WhatsappPhoneNormalizer.isLikelyPhone(from)) {
            return WhatsappPhoneNormalizer.normalize(from);
        }
        if (StringUtils.hasText(waId)) {
            return WhatsappPhoneNormalizer.normalize(waId);
        }
        if (StringUtils.hasText(from)) {
            return WhatsappPhoneNormalizer.normalize(from);
        }
        return null;
    }
}
