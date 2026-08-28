package com.readora.commerce.dto;

import com.readora.commerce.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** returnAgentId/returnAgentName are required when status is RETURN_ASSIGNED, ignored otherwise. */
public record UpdateReturnStatusRequest(
        @NotNull OrderStatus status,
        UUID returnAgentId,
        String returnAgentName
) {
}
