package com.readora.ai.kafka;

// Kafka topic name constants shared by producers and listeners in this service.
public final class KafkaTopics {

    public static final String BOOK_UPSERTED = "book.upserted";
    public static final String EMBEDDING_BACKFILL_REQUESTED = "embedding.backfill.requested";

    // Prevents instantiation of this constants-only holder.
    private KafkaTopics() {
    }
}
