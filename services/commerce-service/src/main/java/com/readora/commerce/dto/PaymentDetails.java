package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mirrors payment-service's PaymentResponse shape — used to enrich order detail with transaction info. */
public record PaymentDetails(
        UUID paymentId,
        UUID orderId,
        String status,
        String method,
        BigDecimal amount,
        BigDecimal walletAmountUsed,
        Instant authorizedAt,
        Instant capturedAt
) {
}
