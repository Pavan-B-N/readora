package com.readora.notification.dto;

import java.util.UUID;

public record PaymentFailedEvent(UUID orderId, UUID userId, String failureCode, String failureReason) {
}
