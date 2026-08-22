package com.readora.auth.dto;

/**
 * One per-field validation failure, nested inside a VALIDATION_FAILED {@link ErrorResponse}.
 *
 * @param field   the name of the request field that failed validation
 * @param message a human-readable description of why it failed
 */
public record FieldErrorItem(
        String field,
        String message
) {
}
