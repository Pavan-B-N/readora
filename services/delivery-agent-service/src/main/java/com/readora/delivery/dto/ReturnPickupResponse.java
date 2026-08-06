package com.readora.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// A return pickup assignment as seen by an agent, from creation through claim to collected.
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
        String recipientName,
        String recipientPhone,
        List<ItemSnapshot> items,
        BigDecimal payoutAmount
) {
}
