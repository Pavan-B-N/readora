package com.readora.delivery.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        int status,
        String path,
        String traceId,
        Instant timestamp,
        List<FieldErrorItem> fieldErrors
) {
    public ErrorResponse(String error, String message, int status, String path, String traceId, Instant timestamp) {
        this(error, message, status, path, traceId, timestamp, null);
    }
}
