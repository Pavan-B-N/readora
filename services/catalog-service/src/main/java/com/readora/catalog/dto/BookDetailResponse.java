package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookDetailResponse(
        UUID id,
        String isbn13,
        String title,

        String description,
        List<AuthorRef> authors,
        CategoryRef category,
        PublisherRef publisher,
        StoreRef store,
        Integer pageCount,
        String language,
        LocalDate publishedOn,
        BigDecimal listPrice,
        String currency,
        List<String> images,
        Availability availability,
        int estimatedDeliveryDays,
        VirtualEditionRef virtualEdition,
        List<String> topics,
        Double averageRating,
        long reviewCount
) {
    public record AuthorRef(UUID id, String name, String bio, String photoUrl) {
    }

    public record CategoryRef(UUID id, String name) {
    }

    public record PublisherRef(UUID id, String name) {
    }

    /** Null for a virtual-only book — it has no physical stocking location. */
    public record StoreRef(UUID id, String name, String city) {
    }

    /**
     * status is one of IN_STOCK, OUT_OF_STOCK (physical, at the caller's store, sold out),
     * NOT_AVAILABLE_AT_STORE (physical, but stocked at a different store than the caller's), or
     * NO_PHYSICAL_EDITION (this book has no physical stocking at all — virtual-only).
     */
    public record Availability(String status, int quantityAvailable) {
    }

    /** Null when no active virtual edition exists — deliberately no fileUrl (never handed out directly). */
    public record VirtualEditionRef(BigDecimal price, String currency) {
    }
}
