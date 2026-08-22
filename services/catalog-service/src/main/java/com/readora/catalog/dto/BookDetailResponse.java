package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BookDetailResponse(
        UUID id,
        String isbn13,
        String title,
        String subtitle,
        String description,
        List<AuthorRef> authors,
        CategoryRef category,
        PublisherRef publisher,
        String format,
        Integer pageCount,
        String language,
        LocalDate publishedOn,
        BigDecimal listPrice,
        String currency,
        List<String> images,
        Availability availability,
        int estimatedDeliveryDays
) {
    public record AuthorRef(UUID id, String name) {
    }

    public record CategoryRef(UUID id, String name) {
    }

    public record PublisherRef(UUID id, String name) {
    }

    public record Availability(String status, int quantityAvailable) {
    }
}
