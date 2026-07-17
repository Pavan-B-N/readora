package com.readora.auth.dto;

/** Response body for a successful POST /api/v1/auth/login. */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
