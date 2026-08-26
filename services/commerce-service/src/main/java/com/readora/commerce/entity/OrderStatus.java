package com.readora.commerce.entity;

/**
 * PENDING_PAYMENT -> PAID -> CONFIRMED -> SHIPPED -> DELIVERED, or -> PAYMENT_FAILED, or ->
 * CANCELLED (only while under 48h old and not yet SHIPPED). A DELIVERED order can move to
 * RETURNED within the return window. Transitions are enforced in OrderService, not by this enum.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED,
    RETURNED
}
