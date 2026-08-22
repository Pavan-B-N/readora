package com.readora.catalog.dto;

import jakarta.validation.constraints.Min;

public record UpdateInventoryRequest(@Min(0) int qtyOnHand, @Min(0) int reorderThreshold) {
}
