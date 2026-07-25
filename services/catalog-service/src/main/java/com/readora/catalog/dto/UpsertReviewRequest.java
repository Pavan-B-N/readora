package com.readora.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Request to create or update a user's review of a book.
public record UpsertReviewRequest(@Min(1) @Max(5) int rating, String comment) {
}
