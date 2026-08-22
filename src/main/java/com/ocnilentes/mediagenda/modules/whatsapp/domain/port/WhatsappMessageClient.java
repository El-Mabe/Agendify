package com.ocnilentes.mediagenda.modules.whatsapp.domain.port;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.model.ReplyButton;
import java.util.List;

public interface WhatsappMessageClient {

    String sendTextMessage(String to, String body);

    String sendInteractiveButtons(String to, String body, List<ReplyButton> buttons);

    String sendTemplate(
            String to,
            String templateName,
            String languageCode,
            List<String> bodyParameters
    );
}
