package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Virtual-edition availability and pricing for the requested books.
public record VirtualEditionLookupResponse(List<Item> items) {
    // One book's virtual-edition availability and price.
    public record Item(UUID bookId, String title, boolean available, BigDecimal price, String currency) {
    }
}
