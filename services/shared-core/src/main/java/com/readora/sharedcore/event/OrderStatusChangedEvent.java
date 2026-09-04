package com.readora.sharedcore.event;

import java.util.UUID;

/**
 * Published by commerce-service on every order status transition — drives notification-service's
 * notification feed generically. deliveryType/storeId let delivery-agent-service filter to
 * "physical order just reached CONFIRMED" without a callback into commerce-service; storeId is
 * null for VIRTUAL orders.
 */
public record OrderStatusChangedEvent(
        UUID orderId, UUID userId, String orderNumber, String toStatus, String deliveryType, UUID storeId
) {
}
