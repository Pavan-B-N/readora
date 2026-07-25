package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

// Admin request to update an existing author.
public record UpdateAuthorRequest(@NotBlank String name, @NotBlank String slug, String bio, String photoUrl) {
}
