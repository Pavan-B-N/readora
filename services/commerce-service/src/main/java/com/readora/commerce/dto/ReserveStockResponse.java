package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Result of a stock reservation, with pricing details for each reserved book.
public record ReserveStockResponse(List<Item> items) {
    // A reserved book's pricing and fulfilling store.
    public record Item(UUID bookId, String title, String isbn13, BigDecimal unitPrice, UUID storeId) {
    }
}
