package com.readora.catalog.kafka;

// Kafka topic name constants used by catalog-service producers/consumers.
public final class KafkaTopics {

    public static final String BOOK_UPSERTED = "book.upserted";

    // Prevents instantiation of this constants-only class.
    private KafkaTopics() {
    }
}
