package com.readora.commerce.dto;

import java.time.Instant;
import java.util.UUID;

// One message in a return's chat thread.
public record ReturnMessageResponse(UUID id, UUID senderUserId, String senderRole, String content, Instant createdAt) {
}
