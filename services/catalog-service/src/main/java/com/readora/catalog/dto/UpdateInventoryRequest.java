package com.readora.catalog.dto;

import jakarta.validation.constraints.Min;

// Admin request to correct a book's on-hand quantity and reorder threshold.
public record UpdateInventoryRequest(@Min(0) int qtyOnHand, @Min(0) int reorderThreshold) {
}
