package com.readora.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/v1/auth/register. fullName is accepted but not currently persisted. */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, message = "must be at least 10 characters") String password,
        @NotBlank String fullName
) {
}
