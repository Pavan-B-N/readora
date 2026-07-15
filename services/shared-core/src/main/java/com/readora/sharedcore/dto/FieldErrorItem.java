package com.readora.sharedcore.dto;

// One field-level validation failure, nested inside an ErrorResponse.
public record FieldErrorItem(String field, String message) {
}
