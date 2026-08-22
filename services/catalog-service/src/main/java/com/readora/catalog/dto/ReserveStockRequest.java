package com.readora.catalog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReserveStockRequest(@NotEmpty List<Item> items) {
    public record Item(@NotNull UUID bookId, int qty) {
    }
}
