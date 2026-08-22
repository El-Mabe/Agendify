package com.ocnilentes.mediagenda.modules.whatsapp.domain.exception;

public class WhatsappApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public WhatsappApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public WhatsappApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.responseBody = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
