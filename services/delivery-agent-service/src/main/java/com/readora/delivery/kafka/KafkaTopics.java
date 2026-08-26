package com.readora.delivery.kafka;

public final class KafkaTopics {

    /** Must match commerce-service's KafkaTopics.ORDER_STATUS_CHANGED — same topic, different service. */
    public static final String ORDER_STATUS_CHANGED = "order.status_changed";

    private KafkaTopics() {
    }
}
