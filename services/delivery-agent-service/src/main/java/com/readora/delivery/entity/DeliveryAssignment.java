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

    // Resolved once at row creation from commerce-service's delivery-detail lookup, not kept live-synced — lets the queue show where a delivery is headed before an agent claims it, without an extra round trip per render.
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

    // No-arg constructor required by JPA.
    protected DeliveryAssignment() {
    }

    // Creates a new unassigned assignment snapshotting the order's delivery detail.
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

    // Stamps createdAt right before the row is inserted.
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Assigns the delivery to an agent and stamps the claim time.
    public void claim(UUID agentId) {
        this.agentId = agentId;
        this.status = DeliveryAssignmentStatus.ASSIGNED;
        this.assignedAt = Instant.now();
    }

    // Marks the delivery as out for delivery and stamps the time.
    public void markOutForDelivery() {
        this.status = DeliveryAssignmentStatus.OUT_FOR_DELIVERY;
        this.outForDeliveryAt = Instant.now();
    }

    // Marks the delivery as delivered and stamps the time.
    public void markDelivered() {
        this.status = DeliveryAssignmentStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    // Returns the assignment id.
    public UUID getId() {
        return id;
    }

    // Returns the id of the underlying order.
    public UUID getOrderId() {
        return orderId;
    }

    // Returns the human-facing order number.
    public String getOrderNumber() {
        return orderNumber;
    }

    // Returns the id of the fulfilling store.
    public UUID getStoreId() {
        return storeId;
    }

    // Returns the snapshotted destination city.
    public String getDestinationCity() {
        return destinationCity;
    }

    // Returns the snapshotted recipient name.
    public String getRecipientName() {
        return recipientName;
    }

    // Returns the snapshotted recipient phone number.
    public String getRecipientPhone() {
        return recipientPhone;
    }

    // Returns the snapshotted items as a JSON array string.
    public String getItemsJson() {
        return itemsJson;
    }

    // Returns the snapshotted payout amount.
    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    // Returns the id of the claiming agent, if any.
    public UUID getAgentId() {
        return agentId;
    }

    // Returns the current assignment status.
    public DeliveryAssignmentStatus getStatus() {
        return status;
    }

    // Returns when the assignment row was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Returns when the assignment was claimed, if any.
    public Instant getAssignedAt() {
        return assignedAt;
    }

    // Returns when it was marked out for delivery, if any.
    public Instant getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    // Returns when it was marked delivered, if any.
    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DeliveryAssignment that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hash based on id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
