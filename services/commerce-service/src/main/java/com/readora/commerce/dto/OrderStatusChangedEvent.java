package com.readora.commerce.dto;

import java.util.UUID;

/** Published on every order status transition — drives notification-service's notification feed generically. */
public record OrderStatusChangedEvent(UUID orderId, UUID userId, String orderNumber, String toStatus) {
}
