package com.readora.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One entry per order that has ever had a refund recorded — orders with no refund yet are simply absent. */
public record RefundStatusResponse(UUID orderId, String status, BigDecimal amount, Instant completedAt) {
}
