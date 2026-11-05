package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

/**
 * Meta Cloud API: {@code to} = teléfono; {@code recipient} = BSUID ({@code CO.123…}).
 * Si se mandan ambos, el teléfono gana y el BSUID se ignora.
 */
record MetaRecipientAddress(String to, String recipient) {

    static MetaRecipientAddress of(String id) {
        if (id != null && id.matches("(?i)^[A-Z]{2}\\.[0-9]+$")) {
            return new MetaRecipientAddress(null, id);
        }
        return new MetaRecipientAddress(id, null);
    }
}
