package com.readora.notification.dto;

import java.util.UUID;

public record OrderStatusChangedEvent(UUID orderId, UUID userId, String orderNumber, String toStatus) {
}
