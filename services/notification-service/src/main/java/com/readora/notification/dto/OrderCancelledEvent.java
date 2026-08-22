package com.readora.notification.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCancelledEvent(UUID orderId, UUID userId, String reason, BigDecimal refundAmount) {
}
