package com.ocnilentes.mediagenda.shared.outbound.google;

public class GoogleSheetsException extends RuntimeException {

    public GoogleSheetsException(String message) {
        super(message);
    }

    public GoogleSheetsException(String message, Throwable cause) {
        super(message, cause);
    }
}
