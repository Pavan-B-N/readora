package com.readora.catalog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

// Request to reserve stock for a batch of books, e.g. when an order is placed.
public record ReserveStockRequest(@NotEmpty List<Item> items) {
    // A single book and quantity to reserve.
    public record Item(@NotNull UUID bookId, int qty) {
    }
}
