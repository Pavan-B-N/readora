package com.readora.delivery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Mirrors commerce-service's event of the same name — only the fields this service reads. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderStatusChangedEvent(
        UUID orderId, UUID userId, String orderNumber, String toStatus, String deliveryType, UUID storeId
) {
}
