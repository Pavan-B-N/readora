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

/** One row per physical order that reached CONFIRMED — created by OrderEventsListener, not seeded. */
@Entity
@Table(name = "delivery_assignments", schema = "delivery")
public class DeliveryAssignment {

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

    @Column(name = "agent_id")
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryAssignmentStatus status = DeliveryAssignmentStatus.UNASSIGNED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "out_for_delivery_at")
    private Instant outForDeliveryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected DeliveryAssignment() {
    }

    public DeliveryAssignment(UUID orderId, String orderNumber, UUID storeId) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.storeId = storeId;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void claim(UUID agentId) {
        this.agentId = agentId;
        this.status = DeliveryAssignmentStatus.ASSIGNED;
        this.assignedAt = Instant.now();
    }

    public void markOutForDelivery() {
        this.status = DeliveryAssignmentStatus.OUT_FOR_DELIVERY;
        this.outForDeliveryAt = Instant.now();
    }

    public void markDelivered() {
        this.status = DeliveryAssignmentStatus.DELIVERED;
        this.deliveredAt = Instant.now();
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

    public UUID getAgentId() {
        return agentId;
    }

    public DeliveryAssignmentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DeliveryAssignment that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
