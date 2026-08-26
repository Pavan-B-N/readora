package com.readora.payment.kafka;

public final class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_RETURNED = "order.returned";
    public static final String PAYMENT_CAPTURED = "payment.captured";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String REFUND_COMPLETED = "refund.completed";

    private KafkaTopics() {
    }
}
