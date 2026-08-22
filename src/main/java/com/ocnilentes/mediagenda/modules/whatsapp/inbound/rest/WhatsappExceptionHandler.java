package com.ocnilentes.mediagenda.modules.whatsapp.inbound.rest;

import com.ocnilentes.mediagenda.modules.whatsapp.domain.exception.WhatsappApiException;
import com.ocnilentes.mediagenda.shared.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WhatsappExceptionHandler {

    @ExceptionHandler(WhatsappApiException.class)
    public ResponseEntity<ApiErrorResponse> handleWhatsappApi(
            WhatsappApiException exception,
            HttpServletRequest request
    ) {
        String message = exception.getMessage();
        if (StringUtils.hasText(exception.getResponseBody())) {
            message = message + ": " + exception.getResponseBody();
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(
                        "WHATSAPP_API_ERROR",
                        message,
                        Instant.now(),
                        request.getRequestURI()
                ));
    }
}
