package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

// Admin request to update an existing category.
public record UpdateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        int displayOrder
) {
}
