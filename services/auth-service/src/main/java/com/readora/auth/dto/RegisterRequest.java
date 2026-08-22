package com.readora.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/auth/register.
 *
 * @param email    the account's email address; must be well-formed and not already registered
 * @param password the account's password; minimum 10 characters
 * @param fullName the registrant's full name (accepted here but not persisted — see build notes)
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, message = "must be at least 10 characters") String password,
        @NotBlank String fullName
) {
}
