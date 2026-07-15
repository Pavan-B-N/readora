package com.readora.sharedcore.dto;

import java.time.Instant;
import java.util.List;

// The JSON body every service returns on an error response.
public record ErrorResponse(
        String error,
        String message,
        int status,
        String path,
        String traceId,
        Instant timestamp,
        List<FieldErrorItem> fieldErrors
) {
    // Convenience overload for errors with no field-level validation detail.
    public ErrorResponse(String error, String message, int status, String path, String traceId, Instant timestamp) {
        this(error, message, status, path, traceId, timestamp, null);
    }
}
