package com.readora.commerce.dto;

import com.readora.commerce.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** deliveryAgentId/deliveryAgentName are required when status is ASSIGNED, ignored otherwise. */
public record UpdateDeliveryStatusRequest(
        @NotNull OrderStatus status,
        UUID deliveryAgentId,
        String deliveryAgentName
) {
}
