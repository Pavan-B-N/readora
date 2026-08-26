package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID orderId,
        String orderNumber,
        String status,
        BigDecimal grandTotal,
        String currency,
        Instant placedAt,
        boolean cancellable,
        Instant deliveredAt
) {
}
