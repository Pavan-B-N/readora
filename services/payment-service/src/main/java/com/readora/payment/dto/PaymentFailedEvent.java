package com.readora.payment.dto;

import java.util.UUID;

public record PaymentFailedEvent(
        UUID orderId,
        UUID userId,
        String failureCode,
        String failureReason
) {
}
