package com.readora.ai.dto;

import java.util.UUID;

// Kafka event signaling that a catalogue embedding backfill job was queued.
public record EmbeddingBackfillRequestedEvent(UUID jobId) {
}
