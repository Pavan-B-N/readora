package com.readora.catalog.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        UUID parentId,
        int displayOrder
) {
}
