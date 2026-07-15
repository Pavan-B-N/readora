package com.readora.sharedcore.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by payment-service; consumed by commerce-service (order state) and user-service (wallet). */
public record PaymentCapturedEvent(
        UUID orderId,
        UUID paymentId,
        UUID userId,
        BigDecimal amount,
        BigDecimal walletAmountUsed
) {
}
