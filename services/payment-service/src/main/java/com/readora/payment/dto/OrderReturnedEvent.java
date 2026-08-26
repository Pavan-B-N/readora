package com.readora.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderReturnedEvent(UUID orderId, UUID userId, String reason, BigDecimal refundAmount) {
}
