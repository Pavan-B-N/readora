package com.readora.payment.entity;

/** INITIATED -> AUTHORIZED -> CAPTURED | FAILED | REFUNDED. Transitions live in PaymentService. */
public enum PaymentStatus {
    INITIATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED
}
