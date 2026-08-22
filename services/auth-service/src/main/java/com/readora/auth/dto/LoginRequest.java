package com.readora.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/v1/auth/login.
 *
 * @param email    the account's email address
 * @param password the account's password
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
