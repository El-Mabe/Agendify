package com.ocnilentes.mediagenda.modules.whatsapp.inbound.rest;

import com.ocnilentes.mediagenda.modules.whatsapp.application.SendTestMessageCommand;
import com.ocnilentes.mediagenda.modules.whatsapp.application.SendTestMessageResult;
import com.ocnilentes.mediagenda.modules.whatsapp.application.SendTestMessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageTestController {

    private static final String DEFAULT_TEST_MESSAGE = "Hola desde MediAgenda Bot";

    private final SendTestMessageService sendTestMessageService;

    public MessageTestController(SendTestMessageService sendTestMessageService) {
        this.sendTestMessageService = sendTestMessageService;
    }

    @PostMapping("/test")
    public ResponseEntity<SendTestMessageResponse> sendTestMessage(
            @Valid @RequestBody SendTestMessageRequest request
    ) {
        String message = StringUtils.hasText(request.message())
                ? request.message()
                : DEFAULT_TEST_MESSAGE;

        SendTestMessageResult result = sendTestMessageService.execute(
                new SendTestMessageCommand(request.to(), message)
        );

        return ResponseEntity.ok(new SendTestMessageResponse(
                "SENT",
                result.messageId(),
                result.to()
        ));
    }
}
