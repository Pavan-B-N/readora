package com.readora.ai.dto;

import java.time.Instant;
import java.util.UUID;

// Summary of one saved conversation for the conversation list view.
public record ConversationSummaryResponse(UUID conversationId, String title, long messageCount, Instant updatedAt) {
}
