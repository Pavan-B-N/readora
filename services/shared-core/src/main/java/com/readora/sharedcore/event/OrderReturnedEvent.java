package com.readora.sharedcore.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published by commerce-service; consumed by payment-service to trigger the refund. */
public record OrderReturnedEvent(UUID orderId, UUID userId, String reason, BigDecimal refundAmount) {
}
