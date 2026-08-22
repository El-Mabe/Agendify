package com.ocnilentes.mediagenda.shared.response;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path
) {
}
