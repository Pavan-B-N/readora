package com.readora.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record EmbeddingJobBookLogResponse(UUID bookId, String title, Instant processedAt) {
}
