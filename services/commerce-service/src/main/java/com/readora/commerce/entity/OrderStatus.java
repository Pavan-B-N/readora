package com.readora.commerce.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED
}
