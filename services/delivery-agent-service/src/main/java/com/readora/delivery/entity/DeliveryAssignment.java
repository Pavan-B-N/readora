package com.readora.delivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
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

    /**
     * Resolved once, when this row is created (see OrderEventsListener), from commerce-service's
     * delivery-detail lookup — not kept live-synced. Lets the queue show an agent where a delivery
     * is headed before they claim it, without an extra network round trip per queue render.
     */
    @Column(name = "destination_city")
    private String destinationCity;

    /** Snapshot of who to hand the package to — same resolve-once-at-creation reasoning as destinationCity. */
    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    /** JSON array of {"title","qty"} objects, snapshotted at creation — parsed back into a list in the service layer for the response. */
    @Column(name = "items_json")
    private String itemsJson;

    /** Snapshotted at creation from the order's value and item count — see OrderEventsListener. */
    @Column(name = "payout_amount")
    private BigDecimal payoutAmount;

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

    public DeliveryAssignment(
            UUID orderId, String orderNumber, UUID storeId, String destinationCity,
            String recipientName, String recipientPhone, String itemsJson, BigDecimal payoutAmount
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.storeId = storeId;
        this.destinationCity = destinationCity;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.itemsJson = itemsJson;
        this.payoutAmount = payoutAmount;
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

    public String getDestinationCity() {
        return destinationCity;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public String getItemsJson() {
        return itemsJson;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
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
