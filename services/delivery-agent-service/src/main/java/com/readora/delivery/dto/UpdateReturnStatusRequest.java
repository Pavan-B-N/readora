package com.readora.delivery.dto;

import java.util.UUID;

/** Mirrors commerce-service's request of the same name — sent to its internal PUT endpoint. */
public record UpdateReturnStatusRequest(String status, UUID returnAgentId, String returnAgentName) {
}
