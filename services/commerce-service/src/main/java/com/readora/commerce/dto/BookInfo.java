package com.readora.commerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookInfo(
        UUID id, String title, String isbn13, BigDecimal listPrice, String currency,
        Availability availability, VirtualEditionInfo virtualEdition
) {
    public record Availability(String status, int quantityAvailable) {
    }

    /** Null when the book has no active virtual edition. */
    public record VirtualEditionInfo(BigDecimal price, String currency) {
    }
}
