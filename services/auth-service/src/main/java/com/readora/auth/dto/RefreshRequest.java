package com.readora.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/v1/auth/refresh.
 *
 * @param refreshToken the raw refresh token previously issued at login or a prior refresh
 */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}
