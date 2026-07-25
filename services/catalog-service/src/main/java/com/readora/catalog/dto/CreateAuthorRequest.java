package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

// Admin request to create a new author.
public record CreateAuthorRequest(@NotBlank String name, @NotBlank String slug, String bio, String photoUrl) {
}
