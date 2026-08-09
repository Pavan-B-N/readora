package com.readora.mcp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

// Full detail for a single book, as returned by catalog-service.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookDetail(
        String id, String title, String publisher, BigDecimal listPrice,
        String currency, Availability availability
) {
    // Stock status for a book.
    public record Availability(String status, int quantityAvailable) {
    }
}
