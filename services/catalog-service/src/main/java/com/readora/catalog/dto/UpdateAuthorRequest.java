package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAuthorRequest(@NotBlank String name, @NotBlank String slug, String bio, String photoUrl) {
}
