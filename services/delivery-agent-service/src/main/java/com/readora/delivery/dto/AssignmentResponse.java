package com.readora.delivery.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID storeId,
        String status,
        Instant createdAt,
        Instant assignedAt,
        Instant outForDeliveryAt,
        Instant deliveredAt
) {
}
