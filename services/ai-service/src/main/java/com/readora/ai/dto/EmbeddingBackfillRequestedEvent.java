package com.readora.ai.dto;

import java.util.UUID;

public record EmbeddingBackfillRequestedEvent(UUID jobId) {
}
