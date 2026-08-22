package com.readora.auth.dto;

import java.time.Instant;
import java.util.List;

/**
 * The shared error envelope every Readora service returns for a non-2xx response.
 *
 * @param error       a stable machine-readable error code, e.g. "INVALID_CREDENTIALS"
 * @param message     a human-readable description of what went wrong
 * @param status      the HTTP status code, duplicated here for convenience
 * @param path        the request path that produced the error
 * @param traceId     the correlation id for this request, for cross-service log lookup
 * @param timestamp   the instant the error was produced
 * @param fieldErrors per-field validation failures, present only for VALIDATION_FAILED; null otherwise
 */
public record ErrorResponse(
        String error,
        String message,
        int status,
        String path,
        String traceId,
        Instant timestamp,
        List<FieldErrorItem> fieldErrors
) {

    /**
     * Convenience constructor for non-validation errors, where there are no field errors to report.
     *
     * @param error     a stable machine-readable error code
     * @param message   a human-readable description of what went wrong
     * @param status    the HTTP status code
     * @param path      the request path that produced the error
     * @param traceId   the correlation id for this request
     * @param timestamp the instant the error was produced
     */
    public ErrorResponse(String error, String message, int status, String path, String traceId, Instant timestamp) {
        this(error, message, status, path, traceId, timestamp, null);
    }
}
