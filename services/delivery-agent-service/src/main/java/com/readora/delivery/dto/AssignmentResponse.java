package com.readora.delivery.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// A delivery assignment as seen by an agent, from creation through claim to delivered.
public record AssignmentResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID storeId,
        String status,
        Instant createdAt,
        Instant assignedAt,
        Instant outForDeliveryAt,
        Instant deliveredAt,
        /** null if commerce-service couldn't be reached when this assignment was created — a display nicety, not required. */
        String destinationCity,
        String recipientName,
        String recipientPhone,
        List<ItemSnapshot> items,
        BigDecimal payoutAmount
) {
}
