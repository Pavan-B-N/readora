package com.readora.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordSearchRequest(@NotBlank String query) {
}
