package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        int displayOrder
) {
}
