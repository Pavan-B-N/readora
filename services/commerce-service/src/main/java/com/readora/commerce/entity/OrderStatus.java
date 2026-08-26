package com.readora.commerce.entity;

/**
 * PENDING_PAYMENT -> PAID -> CONFIRMED -> ASSIGNED -> SHIPPED -> DELIVERED, or -> PAYMENT_FAILED,
 * or -> CANCELLED (only while under 48h old and not yet ASSIGNED). A DELIVERED order can move to
 * RETURNED within the return window. Transitions are enforced in OrderService, not by this enum.
 *
 * ASSIGNED/SHIPPED only apply to PHYSICAL orders, driven by delivery-agent-service via
 * OrderService.updateDeliveryStatus(): ASSIGNED means a delivery agent has claimed the order,
 * SHIPPED means "out for delivery" (kept as SHIPPED rather than a new constant so the existing
 * cancellation/notification logic that already treats it as "in transit" needs no changes).
 * VIRTUAL orders skip both and go CONFIRMED -> DELIVERED immediately.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    ASSIGNED,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED,
    RETURNED
}
