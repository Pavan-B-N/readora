package com.readora.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReturnPickupResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID storeId,
        String status,
        Instant createdAt,
        Instant assignedAt,
        Instant enRouteAt,
        Instant collectedAt,
        String destinationCity,
        BigDecimal payoutAmount
) {
}
