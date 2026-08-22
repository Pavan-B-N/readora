package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReserveStockResponse(List<Item> items) {
    public record Item(UUID bookId, String title, String isbn13, BigDecimal unitPrice) {
    }
}
