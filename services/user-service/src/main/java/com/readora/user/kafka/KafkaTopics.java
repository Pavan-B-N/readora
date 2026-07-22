package com.readora.user.kafka;

// Kafka topic name constants consumed by this service.
public final class KafkaTopics {

    public static final String PAYMENT_CAPTURED = "payment.captured";
    public static final String REFUND_COMPLETED = "refund.completed";

    // Prevents instantiation.
    private KafkaTopics() {
    }
}
