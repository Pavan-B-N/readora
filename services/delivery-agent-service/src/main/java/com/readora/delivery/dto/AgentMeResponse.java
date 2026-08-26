package com.readora.delivery.dto;

import java.util.UUID;

public record AgentMeResponse(UUID userId, String name, String phone, UUID storeId) {
}
