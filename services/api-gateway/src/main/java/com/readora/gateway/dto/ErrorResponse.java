package com.readora.gateway.dto;

import java.time.Instant;

/** Standard error response returned by Readora services. */
public record ErrorResponse(
        String error,
        String message,
        int status,
        String path,
        String traceId,
        Instant timestamp
) {
}
