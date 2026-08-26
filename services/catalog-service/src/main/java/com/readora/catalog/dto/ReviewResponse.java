package com.readora.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID userId,
        String authorDisplayName,
        int rating,
        String comment,
        boolean verifiedPurchase,
        Instant createdAt
) {
}
