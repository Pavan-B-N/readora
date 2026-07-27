package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Response carrying virtual edition details for the requested book ids.
public record VirtualEditionLookupResponse(List<Item> items) {
    // One book's virtual edition lookup result.
    public record Item(UUID bookId, String title, boolean available, BigDecimal price, String currency) {
    }
}
