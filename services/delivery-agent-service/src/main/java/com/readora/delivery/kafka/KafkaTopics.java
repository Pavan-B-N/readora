package com.readora.delivery.kafka;

// Kafka topic name constants used by this service.
public final class KafkaTopics {

    /** Must match commerce-service's KafkaTopics.ORDER_STATUS_CHANGED — same topic, different service. */
    public static final String ORDER_STATUS_CHANGED = "order.status_changed";

    // Prevents instantiation of this constants holder.
    private KafkaTopics() {
    }
}
