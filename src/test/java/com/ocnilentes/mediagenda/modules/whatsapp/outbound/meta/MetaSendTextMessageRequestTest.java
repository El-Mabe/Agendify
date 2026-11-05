package com.ocnilentes.mediagenda.modules.whatsapp.outbound.meta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MetaSendTextMessageRequestTest {

    @Test
    void phoneUsesToAndOmitsRecipient() {
        MetaSendTextMessageRequest request = MetaSendTextMessageRequest.of("573186855763", "Hola");
        assertThat(request.to()).isEqualTo("573186855763");
        assertThat(request.recipient()).isNull();
    }

    @Test
    void bsuidUsesRecipientAndOmitsTo() {
        MetaSendTextMessageRequest request = MetaSendTextMessageRequest.of("CO.1142953118206038", "Hola");
        assertThat(request.to()).isNull();
        assertThat(request.recipient()).isEqualTo("CO.1142953118206038");
    }
}
