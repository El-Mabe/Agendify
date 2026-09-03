package com.ocnilentes.mediagenda.modules.whatsapp.inbound.rest;

import com.ocnilentes.mediagenda.modules.whatsapp.application.ProcessIncomingWebhookService;
import com.ocnilentes.mediagenda.modules.whatsapp.domain.exception.WhatsappApiException;
import com.ocnilentes.mediagenda.modules.whatsapp.inbound.webhook.MetaWebhookPayload;
import com.ocnilentes.mediagenda.shared.config.WhatsappProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);
    private static final String SUBSCRIBE_MODE = "subscribe";
    private static final int RAW_PAYLOAD_LOG_LIMIT = 4000;

    private final WhatsappProperties whatsappProperties;
    private final ProcessIncomingWebhookService processIncomingWebhookService;

    public WhatsappWebhookController(
            WhatsappProperties whatsappProperties,
            ProcessIncomingWebhookService processIncomingWebhookService
    ) {
        this.whatsappProperties = whatsappProperties;
        this.processIncomingWebhookService = processIncomingWebhookService;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (!SUBSCRIBE_MODE.equals(mode) || !StringUtils.hasText(challenge)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        String configuredToken = trimToNull(whatsappProperties.verifyToken());
        String receivedToken = trimToNull(verifyToken);

        if (configuredToken == null || !configuredToken.equals(receivedToken)) {
            log.warn(
                    "Webhook verification failed: configuredTokenPresent={} receivedToken='{}'",
                    configuredToken != null,
                    receivedToken
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        log.info("Webhook verified successfully");
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody(required = false) String rawBody) {
        log.info("Webhook POST received");
        if (StringUtils.hasText(rawBody)) {
            log.info("Webhook raw payload={}", truncate(rawBody));
        }

        try {
            processIncomingWebhookService.processRaw(rawBody);
        } catch (WhatsappApiException exception) {
            log.error(
                    "Failed to process WhatsApp webhook payload: status={} body={}",
                    exception.getStatusCode(),
                    exception.getResponseBody(),
                    exception
            );
        } catch (Exception exception) {
            log.error("Failed to process WhatsApp webhook payload", exception);
        }

        return ResponseEntity.ok().build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value) {
        if (value.length() <= RAW_PAYLOAD_LOG_LIMIT) {
            return value;
        }
        return value.substring(0, RAW_PAYLOAD_LOG_LIMIT) + "...(truncated)";
    }
}
