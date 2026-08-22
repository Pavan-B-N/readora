package com.readora.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(UUID conversationId, String title, long messageCount, Instant updatedAt) {
}
