package com.readora.delivery.dto;

import java.util.UUID;

// A delivery agent as seen by an admin, with whatever it's currently carrying, if anything.
public record AdminAgentResponse(UUID userId, String name, String phone, boolean onDuty, ActiveWork activeWork) {
    /** type is "DELIVERY" or "RETURN_PICKUP" — null activeWork on the parent means the agent has nothing in flight right now. */
    public record ActiveWork(String type, String orderNumber, String status, String destinationCity) {
    }
}
