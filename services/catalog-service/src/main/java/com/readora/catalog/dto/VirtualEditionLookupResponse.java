package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VirtualEditionLookupResponse(List<Item> items) {
    public record Item(UUID bookId, String title, boolean available, BigDecimal price, String currency) {
    }
}
