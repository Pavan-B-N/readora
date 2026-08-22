package com.readora.catalog.dto;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String slug, List<CategoryResponse> children) {
}
