package com.readora.ai.kafka;

public final class KafkaTopics {

    public static final String BOOK_UPSERTED = "book.upserted";
    public static final String EMBEDDING_BACKFILL_REQUESTED = "embedding.backfill.requested";

    private KafkaTopics() {
    }
}
