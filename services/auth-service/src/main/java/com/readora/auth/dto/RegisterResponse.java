package com.readora.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String email,
        Instant createdAt
) {
}
