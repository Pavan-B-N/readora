package com.readora.auth.dto;

import java.time.Instant;
import java.util.UUID;

/** Response body for a successful POST /api/v1/auth/register. */
public record RegisterResponse(
        UUID userId,
        String email,
        Instant createdAt
) {
}
