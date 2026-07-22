package com.readora.user.dto;

import jakarta.validation.constraints.NotBlank;

// Request body for recording a search term.
public record RecordSearchRequest(@NotBlank String query) {
}
