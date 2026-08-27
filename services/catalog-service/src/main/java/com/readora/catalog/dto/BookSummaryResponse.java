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
        String category,
        BigDecimal listPrice,
        String currency,
        String coverImageUrl,
        String availability,
        boolean hasVirtualEdition,
        /** PHYSICAL or VIRTUAL — which edition *this* listing represents, so a caller (e.g. a quick "Add to cart" button) knows which deliveryType to send. Distinct from hasVirtualEdition, which just says a virtual edition also exists. */
        String deliveryType,
        Double averageRating,
        long reviewCount
) {
}
