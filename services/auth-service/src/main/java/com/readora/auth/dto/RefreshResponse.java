package com.readora.auth.dto;

/**
 * Response body for a successful POST /api/v1/auth/refresh.
 *
 * @param accessToken  a newly signed short-lived JWT
 * @param refreshToken a newly issued refresh token — the previous one is now revoked
 * @param expiresIn    the new access token's lifetime in seconds from the moment of issue
 */
public record RefreshResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
