package com.readora.delivery.dto;

import java.util.UUID;

// The caller's own agent profile.
public record AgentMeResponse(UUID userId, String name, String phone, UUID storeId, boolean onDuty) {
}
