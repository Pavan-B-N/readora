package com.readora.ai.dto;

import com.readora.ai.entity.EmbeddingJobStatus;

import java.time.Instant;
import java.util.UUID;

// Status and progress snapshot of one embedding backfill job.
public record EmbeddingJobResponse(
        UUID id,
        EmbeddingJobStatus status,
        int totalBooks,
        int processedBooks,
        String currentBookTitle,
        String errorMessage,
        Instant queuedAt,
        Instant startedAt,
        Instant finishedAt
) {
}
