package com.readora.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundCompletedEvent(
        UUID orderId,
        UUID refundId,
        UUID userId,
        BigDecimal amount,
        BigDecimal walletAmountToReverse
) {
}
