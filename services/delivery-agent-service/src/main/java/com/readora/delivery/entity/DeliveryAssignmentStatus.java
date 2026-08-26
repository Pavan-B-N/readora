package com.readora.delivery.entity;

/**
 * UNASSIGNED -> ASSIGNED -> OUT_FOR_DELIVERY -> DELIVERED. This service's own bookkeeping copy
 * of an order's delivery progress — commerce-service's Order.status stays the source of truth
 * for what the customer sees; this drives the agent-facing queue/claim UX.
 */
public enum DeliveryAssignmentStatus {
    UNASSIGNED,
    ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED
}
