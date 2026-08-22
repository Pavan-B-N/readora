package com.readora.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/v1/auth/logout.
 *
 * @param refreshToken the raw refresh token to revoke
 */
public record LogoutRequest(
        @NotBlank String refreshToken
) {
}
