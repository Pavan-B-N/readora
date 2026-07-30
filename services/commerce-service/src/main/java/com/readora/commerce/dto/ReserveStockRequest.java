package com.readora.commerce.dto;

import java.util.List;
import java.util.UUID;

// Request to catalog-service to reserve stock for a set of books.
public record ReserveStockRequest(List<Item> items) {
    // One book and the quantity to reserve.
    public record Item(UUID bookId, int qty) {
    }
}
