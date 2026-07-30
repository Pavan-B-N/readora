package com.readora.commerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

// Mirrors catalog-service's book response used by checkout and cart validation.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookInfo(
        UUID id, String title, String isbn13, BigDecimal listPrice, String currency,
        Availability availability, VirtualEditionInfo virtualEdition
) {
    // A book's stock status and quantity at the relevant store.
    public record Availability(String status, int quantityAvailable) {
    }

    /** Null when the book has no active virtual edition. */
    public record VirtualEditionInfo(BigDecimal price, String currency) {
    }
}
