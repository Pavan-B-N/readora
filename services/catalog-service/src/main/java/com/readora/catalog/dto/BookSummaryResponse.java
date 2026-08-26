package com.readora.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BookSummaryResponse(
        UUID id,
        String isbn13,
        String title,
        List<String> authors,
        String publisher,
        BigDecimal listPrice,
        String currency,
        String coverImageUrl,
        String availability,
        Double averageRating,
        long reviewCount
) {
}
