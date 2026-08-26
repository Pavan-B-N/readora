package com.readora.commerce.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mirrors payment-service's response of the same shape. */
public record RefundStatus(UUID orderId, String status, BigDecimal amount, Instant completedAt) {
}
