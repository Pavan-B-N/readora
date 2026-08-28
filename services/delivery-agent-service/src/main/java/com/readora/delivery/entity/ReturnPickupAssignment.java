package com.readora.delivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One row per order whose return reached RETURN_APPROVED — created by OrderEventsListener, not
 * seeded. Separate entity from DeliveryAssignment (not a repurposing of it): its orderId-unique
 * constraint and status names are hard-wired to one-per-order forward delivery, and this is a
 * distinct leg (pickup, not drop-off) that can happen after a DeliveryAssignment already reached
 * DELIVERED for the same order.
 */
@Entity
@Table(name = "return_pickup_assignments", schema = "delivery")
public class ReturnPickupAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    /** Resolved once at creation time — see DeliveryAssignment's identical field for why. */
    @Column(name = "destination_city")
    private String destinationCity;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "agent_name")
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnPickupStatus status = ReturnPickupStatus.UNASSIGNED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "en_route_at")
    private Instant enRouteAt;

    @Column(name = "collected_at")
    private Instant collectedAt;

    protected ReturnPickupAssignment() {
    }

    public ReturnPickupAssignment(UUID orderId, String orderNumber, UUID storeId, String destinationCity) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.storeId = storeId;
        this.destinationCity = destinationCity;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void claim(UUID agentId, String agentName) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.status = ReturnPickupStatus.ASSIGNED;
        this.assignedAt = Instant.now();
    }

    public void markEnRoute() {
        this.status = ReturnPickupStatus.EN_ROUTE;
        this.enRouteAt = Instant.now();
    }

    public void markCollected() {
        this.status = ReturnPickupStatus.COLLECTED;
        this.collectedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public ReturnPickupStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getEnRouteAt() {
        return enRouteAt;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ReturnPickupAssignment that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
