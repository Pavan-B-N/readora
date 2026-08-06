package com.readora.delivery.entity;

// UNASSIGNED -> ASSIGNED -> EN_ROUTE -> COLLECTED — this service's own bookkeeping copy of return pickup progress; mirrors DeliveryAssignmentStatus but kept separate since a pickup is a distinct entity from a forward delivery.
public enum ReturnPickupStatus {
    UNASSIGNED,
    ASSIGNED,
    EN_ROUTE,
    COLLECTED
}
