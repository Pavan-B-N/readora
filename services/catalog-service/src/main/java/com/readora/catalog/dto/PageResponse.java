package com.readora.catalog.dto;

import java.util.List;

// Generic paginated response wrapper.
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
}
