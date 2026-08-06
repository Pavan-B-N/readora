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

// One row per order whose return reached RETURN_APPROVED, created by OrderEventsListener; kept separate from DeliveryAssignment since its unique-per-order constraint and status names are hard-wired to forward delivery, and a pickup can happen after that already reached DELIVERED for the same order.
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

    // No-arg constructor required by JPA.
    protected ReturnPickupAssignment() {
    }

    // Creates a new unassigned return pickup snapshotting the order's delivery detail.
    public ReturnPickupAssignment(
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

    // Assigns the pickup to an agent and stamps the claim time.
    public void claim(UUID agentId, String agentName) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.status = ReturnPickupStatus.ASSIGNED;
        this.assignedAt = Instant.now();
    }

    // Marks the agent as en route and stamps the time.
    public void markEnRoute() {
        this.status = ReturnPickupStatus.EN_ROUTE;
        this.enRouteAt = Instant.now();
    }

    // Marks the item as collected and stamps the time.
    public void markCollected() {
        this.status = ReturnPickupStatus.COLLECTED;
        this.collectedAt = Instant.now();
    }

    // Returns the pickup assignment id.
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

    // Returns the name of the claiming agent, if any.
    public String getAgentName() {
        return agentName;
    }

    // Returns the current pickup status.
    public ReturnPickupStatus getStatus() {
        return status;
    }

    // Returns when the assignment row was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Returns when the pickup was claimed, if any.
    public Instant getAssignedAt() {
        return assignedAt;
    }

    // Returns when the agent was marked en route, if any.
    public Instant getEnRouteAt() {
        return enRouteAt;
    }

    // Returns when the item was marked collected, if any.
    public Instant getCollectedAt() {
        return collectedAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ReturnPickupAssignment that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hash based on id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
