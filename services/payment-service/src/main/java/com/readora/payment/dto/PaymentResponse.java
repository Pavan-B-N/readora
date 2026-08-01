package com.readora.payment.dto;

import com.readora.payment.entity.PaymentMethod;
import com.readora.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Response body describing a payment's current state, including its refund if one exists.
public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        PaymentStatus status,
        PaymentMethod method,
        BigDecimal amount,
        BigDecimal walletAmountUsed,
        Instant authorizedAt,
        Instant capturedAt,
        RefundInfo refund
) {
    // Refund summary embedded in a payment response.
    public record RefundInfo(String status, BigDecimal amount) {
    }
}
