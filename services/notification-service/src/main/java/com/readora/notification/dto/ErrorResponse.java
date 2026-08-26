package com.readora.notification.dto;

import java.time.Instant;

public record ErrorResponse(
        String error,
        String message,
        int status,
        String path,
        String traceId,
        Instant timestamp
) {
}
