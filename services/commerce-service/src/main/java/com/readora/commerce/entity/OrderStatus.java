package com.readora.commerce.entity;

// PENDING_PAYMENT -> PAID -> CONFIRMED -> ASSIGNED -> SHIPPED -> DELIVERED (or -> PAYMENT_FAILED / CANCELLED within 48h pre-ASSIGNED), transitions enforced in OrderFulfillmentService/ReturnService; ASSIGNED/SHIPPED apply only to PHYSICAL orders and are driven by delivery-agent-service via updateDeliveryStatus() (VIRTUAL orders skip straight from CONFIRMED to DELIVERED); a DELIVERED order can move to RETURN_REQUESTED within the return window, after which VIRTUAL orders auto-advance RETURN_REQUESTED -> REFUND_INITIATED -> RETURNED with no admin step, while orders with a physical item wait for an admin decision (chat open via ReturnMessage) that either rejects terminally (RETURN_REJECTED) or approves and runs RETURN_APPROVED -> RETURN_ASSIGNED -> RETURN_EN_ROUTE -> RETURN_COLLECTED -> REFUND_INITIATED -> RETURNED via updateReturnPickupStatus(); RETURNED is reached only once payment-service confirms refund.completed (handleRefundCompleted()), the one terminal "fully refunded" status for both paths; ebook access is revoked from RETURN_APPROVED onward, not RETURN_REQUESTED, so a customer keeps reading while a return is pending review.
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    ASSIGNED,
    SHIPPED,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURN_REJECTED,
    RETURN_APPROVED,
    RETURN_ASSIGNED,
    RETURN_EN_ROUTE,
    RETURN_COLLECTED,
    REFUND_INITIATED,
    RETURNED
}
