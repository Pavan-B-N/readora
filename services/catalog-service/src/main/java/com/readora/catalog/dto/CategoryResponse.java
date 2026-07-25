package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

// API response for a category.
public record CategoryResponse(UUID id, String name, String slug, int displayOrder, List<CategoryResponse> children) {
}
