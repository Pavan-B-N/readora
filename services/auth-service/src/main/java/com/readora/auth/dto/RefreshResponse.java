package com.readora.auth.dto;

/** Response body for a successful POST /api/v1/auth/refresh. The previous refresh token is now revoked. */
public record RefreshResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
