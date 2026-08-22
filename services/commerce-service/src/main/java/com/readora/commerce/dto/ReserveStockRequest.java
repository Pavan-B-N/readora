package com.readora.commerce.dto;

import java.util.List;
import java.util.UUID;

public record ReserveStockRequest(List<Item> items) {
    public record Item(UUID bookId, int qty) {
    }
}
