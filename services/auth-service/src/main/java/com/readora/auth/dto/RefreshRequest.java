package com.readora.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /api/v1/auth/refresh. */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}
