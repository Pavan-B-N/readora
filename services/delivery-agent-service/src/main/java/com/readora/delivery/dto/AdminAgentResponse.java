package com.readora.delivery.dto;

import java.util.UUID;

public record AdminAgentResponse(UUID userId, String name, String phone, boolean onDuty, ActiveWork activeWork) {
    /** type is "DELIVERY" or "RETURN_PICKUP" — null activeWork on the parent means the agent has nothing in flight right now. */
    public record ActiveWork(String type, String orderNumber, String status, String destinationCity) {
    }
}
