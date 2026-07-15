package com.readora.sharedcore.event;

import java.util.UUID;

// Published by commerce-service on every status transition; deliveryType/storeId let delivery-agent-service filter without a callback (storeId is null for VIRTUAL orders).
public record OrderStatusChangedEvent(
        UUID orderId, UUID userId, String orderNumber, String toStatus, String deliveryType, UUID storeId
) {
}
