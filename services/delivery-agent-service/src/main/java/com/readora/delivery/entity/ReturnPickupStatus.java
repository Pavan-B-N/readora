package com.readora.delivery.entity;

/**
 * UNASSIGNED -> ASSIGNED -> EN_ROUTE -> COLLECTED. This service's own bookkeeping copy of a
 * return pickup's progress — commerce-service's Order.status stays the source of truth for what
 * the customer sees; this drives the agent-facing queue/claim UX. Mirrors DeliveryAssignmentStatus
 * but kept separate since a return pickup is a distinct entity from a forward delivery.
 */
public enum ReturnPickupStatus {
    UNASSIGNED,
    ASSIGNED,
    EN_ROUTE,
    COLLECTED
}
