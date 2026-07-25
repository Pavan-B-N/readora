package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

// Admin request to create a new category.
public record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        int displayOrder
) {
}
