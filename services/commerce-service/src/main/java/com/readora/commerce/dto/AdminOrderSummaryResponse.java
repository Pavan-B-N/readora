package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderSummaryResponse(
        UUID orderId,
        String orderNumber,
        String status,
        BigDecimal grandTotal,
        String currency,
        Instant placedAt,
        Instant cancelledAt,
        String cancelReason,
        String refundStatus,
        BigDecimal refundAmount,
        Instant refundCompletedAt,
        Instant adminReviewedAt,
        String adminNote
) {
}
