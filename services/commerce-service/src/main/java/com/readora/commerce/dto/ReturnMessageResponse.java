package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

public record ReturnMessageResponse(UUID id, UUID senderUserId, String senderRole, String content, Instant createdAt) {
}
