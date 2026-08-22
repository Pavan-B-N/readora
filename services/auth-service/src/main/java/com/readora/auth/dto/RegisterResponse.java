package com.readora.auth.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for a successful POST /api/v1/auth/register.
 *
 * @param userId    the newly created account's id
 * @param email     the newly created account's email address
 * @param createdAt the instant the account was created
 */
public record RegisterResponse(
        UUID userId,
        String email,
        Instant createdAt
) {
}
