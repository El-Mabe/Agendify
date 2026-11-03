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

        // LID / BSUID: no anteponer 57 (rompería el id y Meta rechaza el envío).
        if (isLikelyOpaqueId(phone, digits)) {
            return digits;
        }

        // Celular Colombia local: 3XXXXXXXXX (10 dígitos) → 57XXXXXXXXXX
        if (digits.length() == 10 && digits.startsWith("3")) {
            return "57" + digits;
        }

        return digits;
    }

    static boolean isLikelyPhone(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String digits = value.trim().replaceAll("[^0-9]", "");
        if (isLikelyOpaqueId(value, digits)) {
            return false;
        }
        if (digits.length() == 10 && digits.startsWith("3")) {
            return true;
        }
        return digits.length() >= 11 && digits.length() <= 13;
    }

    private static boolean isLikelyOpaqueId(String raw, String digits) {
        String lower = raw.toLowerCase();
        if (lower.contains("lid") || lower.contains("bsuid") || lower.startsWith("rs.")) {
            return true;
        }
        return digits.length() >= 14;
    }
}
