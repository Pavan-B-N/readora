package com.readora.gateway.dto;

import java.time.Instant;

/**
 * Mirrors the shared error envelope every Readora service returns (see auth-service's
 * ErrorResponse). Duplicated here rather than shared across modules — no shared library
 * module exists yet, and this is small enough that duplicating beats introducing one for now.
 */
public record ErrorResponse(
        String error,
        String message,
        int status,
        String path,
        String traceId,
        Instant timestamp
) {
}
