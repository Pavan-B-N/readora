package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAuthorRequest(@NotBlank String name, @NotBlank String slug, String bio) {
}
