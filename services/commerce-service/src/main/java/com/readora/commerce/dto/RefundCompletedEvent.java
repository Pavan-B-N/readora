package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Mirrors payment-service's event of the same shape. */
public record RefundCompletedEvent(
        UUID orderId,
        UUID refundId,
        UUID userId,
        BigDecimal amount,
        BigDecimal walletAmountToReverse
) {
}
