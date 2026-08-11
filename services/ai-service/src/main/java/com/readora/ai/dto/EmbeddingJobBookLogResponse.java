package com.readora.ai.dto;

import java.time.Instant;
import java.util.UUID;

// One book processed by a backfill job, for the job's book-by-book log feed.
public record EmbeddingJobBookLogResponse(UUID bookId, String title, Instant processedAt) {
}
