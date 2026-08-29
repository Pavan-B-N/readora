package com.readora.commerce.kafka;

public final class KafkaTopics {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_RETURNED = "order.returned";
    public static final String ORDER_STATUS_CHANGED = "order.status_changed";
    public static final String PAYMENT_CAPTURED = "payment.captured";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String REFUND_COMPLETED = "refund.completed";
    /** Generic, arbitrarily-targeted notification — used for admin-facing return alerts, unlike ORDER_STATUS_CHANGED which is always the order's own customer. */
    public static final String NOTIFICATION_REQUESTED = "notification.requested";

    private KafkaTopics() {
    }
}
