package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Response confirming stock reservation, with pricing/store details for the order.
public record ReserveStockResponse(List<Item> items) {
    // Details of one reserved book.
    public record Item(UUID bookId, String title, String isbn13, BigDecimal unitPrice, UUID storeId) {
    }
}
