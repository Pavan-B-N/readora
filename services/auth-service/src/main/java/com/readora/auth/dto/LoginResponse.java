package com.readora.auth.dto;

/**
 * Response body for a successful POST /api/v1/auth/login.
 *
 * @param accessToken  a short-lived signed JWT, sent as a Bearer token on subsequent requests
 * @param refreshToken a long-lived opaque token, exchanged at /auth/refresh for a new pair
 * @param tokenType    always "Bearer"
 * @param expiresIn    the access token's lifetime in seconds from the moment of issue
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
