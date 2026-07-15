package com.readora.sharedcore.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by payment-service; consumed by commerce-service, notification-service, and user-service (wallet). */
public record RefundCompletedEvent(
        UUID orderId,
        UUID refundId,
        UUID userId,
        BigDecimal amount,
        BigDecimal walletAmountToReverse
) {
}
