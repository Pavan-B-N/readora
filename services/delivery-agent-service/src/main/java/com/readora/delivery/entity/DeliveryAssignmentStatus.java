package com.readora.delivery.entity;

// UNASSIGNED -> ASSIGNED -> OUT_FOR_DELIVERY -> DELIVERED — this service's own bookkeeping copy of delivery progress driving the agent-facing queue/claim UX; commerce-service's Order.status stays the source of truth for what the customer sees.
public enum DeliveryAssignmentStatus {
    UNASSIGNED,
    ASSIGNED,
    OUT_FOR_DELIVERY,
    DELIVERED
}
