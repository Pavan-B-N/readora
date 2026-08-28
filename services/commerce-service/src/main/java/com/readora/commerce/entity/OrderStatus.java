package com.readora.commerce.entity;

/**
 * PENDING_PAYMENT -> PAID -> CONFIRMED -> ASSIGNED -> SHIPPED -> DELIVERED, or -> PAYMENT_FAILED,
 * or -> CANCELLED (only while under 48h old and not yet ASSIGNED). Transitions are enforced in
 * OrderService, not by this enum.
 *
 * ASSIGNED/SHIPPED only apply to PHYSICAL orders, driven by delivery-agent-service via
 * OrderService.updateDeliveryStatus(): ASSIGNED means a delivery agent has claimed the order,
 * SHIPPED means "out for delivery" (kept as SHIPPED rather than a new constant so the existing
 * cancellation/notification logic that already treats it as "in transit" needs no changes).
 * VIRTUAL orders skip both and go CONFIRMED -> DELIVERED immediately.
 *
 * A DELIVERED order can move to RETURN_REQUESTED within the return window (OrderService.returnOrder()).
 * From there:
 *   - VIRTUAL orders auto-advance instantly: RETURN_REQUESTED -> REFUND_INITIATED -> RETURNED,
 *     no admin step (nothing physical to inspect).
 *   - Orders with a physical item wait for an admin decision (a chat is open on the order during
 *     this state — see ReturnMessage): RETURN_REQUESTED -> RETURN_REJECTED (terminal, no refund),
 *     or RETURN_REQUESTED -> RETURN_APPROVED -> RETURN_ASSIGNED -> RETURN_EN_ROUTE ->
 *     RETURN_COLLECTED -> REFUND_INITIATED -> RETURNED, driven by delivery-agent-service via
 *     OrderService.updateReturnPickupStatus() the same way ASSIGNED/SHIPPED/DELIVERED are.
 * RETURNED itself is only ever reached once payment-service confirms refund.completed
 * (OrderService.handleRefundCompleted()) — it stays the one terminal "fully refunded" status for
 * both paths, so every existing consumer of RETURNED (ebook-access exclusion, admin return list,
 * frontend status coloring) keeps working unchanged.
 *
 * Ebook access (OrderItemRepository.findDistinctBookIdsByUserId) is revoked from RETURN_APPROVED
 * onward, not at RETURN_REQUESTED — a customer keeps reading while their return is pending review.
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
    RETURN_REQUESTED,
    RETURN_REJECTED,
    RETURN_APPROVED,
    RETURN_ASSIGNED,
    RETURN_EN_ROUTE,
    RETURN_COLLECTED,
    REFUND_INITIATED,
    RETURNED
}
