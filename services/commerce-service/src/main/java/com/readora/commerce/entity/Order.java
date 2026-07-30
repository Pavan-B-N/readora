package com.readora.commerce.entity;

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

/** A placed order. Wallet balance is verified synchronously at checkout, before this row exists. */
@Entity
@Table(name = "orders", schema = "commerce")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "packaging_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal packagingFee;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "grand_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "wallet_amount_used", nullable = false, precision = 10, scale = 2)
    private BigDecimal walletAmountUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType;

    /** The store fulfilling this order's physical items — null for VIRTUAL-only orders. */
    @Column(name = "store_id")
    private UUID storeId;

    // Snapshot of whichever delivery agent claimed this order — never a live lookup into delivery-agent-service, same reasoning as OrderItem's title/isbn snapshots.
    @Column(name = "delivery_agent_id")
    private UUID deliveryAgentId;

    @Column(name = "delivery_agent_name")
    private String deliveryAgentName;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    // Snapshot of whichever agent claimed this order's RETURN pickup — separate from deliveryAgentId/deliveryAgentName above so approving a return doesn't overwrite the original forward-delivery agent's snapshot.
    @Column(name = "return_agent_id")
    private UUID returnAgentId;

    @Column(name = "return_agent_name")
    private String returnAgentName;

    // Lets a store admin acknowledge a cancellation/return case with a note — the "way to act on it" for return/refund visibility, since refunds themselves are fully automatic (dummy payment provider, no manual approval gate) and there's no separate dispute-ticket entity.
    @Column(name = "admin_reviewed_at")
    private Instant adminReviewedAt;

    @Column(name = "admin_reviewed_by_user_id")
    private UUID adminReviewedByUserId;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

    // JPA no-arg constructor.
    protected Order() {
    }

    // Creates a new order in PENDING_PAYMENT with its priced totals.
    public Order(
            String orderNumber, UUID userId, String currency, BigDecimal subtotal,
            BigDecimal shippingFee, BigDecimal packagingFee, BigDecimal taxAmount, BigDecimal grandTotal,
            BigDecimal walletAmountUsed, PaymentMethod paymentMethod, String idempotencyKey, DeliveryType deliveryType
    ) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.currency = currency;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.packagingFee = packagingFee;
        this.taxAmount = taxAmount;
        this.grandTotal = grandTotal;
        this.walletAmountUsed = walletAmountUsed;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
        this.deliveryType = deliveryType;
    }

    // Stamps the placement time before insert.
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.placedAt = Instant.now();
    }

    // Moves the order to the given status.
    public void transitionTo(OrderStatus newStatus) {
        this.status = newStatus;
    }

    // Sets the fulfilling store for this order.
    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    // A delivery agent has claimed this physical order.
    public void assignToAgent(UUID agentId, String agentName) {
        this.status = OrderStatus.ASSIGNED;
        this.deliveryAgentId = agentId;
        this.deliveryAgentName = agentName;
    }

    // Out for delivery — kept as the SHIPPED status, see OrderStatus's comment.
    public void markOutForDelivery() {
        this.status = OrderStatus.SHIPPED;
    }

    // Marks the order delivered and records the delivery time.
    public void markDelivered() {
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    // Records an admin's review note on this order's cancellation/return case.
    public void markReviewed(UUID reviewerId, String note) {
        this.adminReviewedAt = Instant.now();
        this.adminReviewedByUserId = reviewerId;
        this.adminNote = note;
    }

    // Cancels the order with a reason.
    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelReason = reason;
    }

    // Whether the order is still within the 48h cancel window and hasn't progressed past pre-shipment.
    public boolean isCancellable() {
        boolean withinWindow = placedAt.isAfter(Instant.now().minus(java.time.Duration.ofHours(48)));
        // DELIVERED already covers every return-family status too (a return only starts from DELIVERED and none of the later statuses go back to it), but listed explicitly rather than relying on that chain so this stays correct if the flow ever changes.
        boolean notAssignedOrBeyond = status != OrderStatus.ASSIGNED
                && status != OrderStatus.SHIPPED
                && status != OrderStatus.DELIVERED
                && status != OrderStatus.CANCELLED
                && status != OrderStatus.RETURN_REQUESTED
                && status != OrderStatus.RETURN_REJECTED
                && status != OrderStatus.RETURN_APPROVED
                && status != OrderStatus.RETURN_ASSIGNED
                && status != OrderStatus.RETURN_EN_ROUTE
                && status != OrderStatus.RETURN_COLLECTED
                && status != OrderStatus.REFUND_INITIATED
                && status != OrderStatus.RETURNED;
        return withinWindow && notAssignedOrBeyond;
    }

    // Customer submitted a return request — awaiting either auto-advance (virtual) or admin review (physical).
    public void requestReturn(String reason) {
        this.status = OrderStatus.RETURN_REQUESTED;
        this.cancelledAt = Instant.now();
        this.cancelReason = reason;
    }

    // Admin approved the return request.
    public void approveReturn() {
        this.status = OrderStatus.RETURN_APPROVED;
    }

    // Admin rejected the return request.
    public void rejectReturn() {
        this.status = OrderStatus.RETURN_REJECTED;
    }

    // A delivery agent has claimed this return's pickup.
    public void assignReturnAgent(UUID agentId, String agentName) {
        this.status = OrderStatus.RETURN_ASSIGNED;
        this.returnAgentId = agentId;
        this.returnAgentName = agentName;
    }

    // Return pickup is en route.
    public void markReturnEnRoute() {
        this.status = OrderStatus.RETURN_EN_ROUTE;
    }

    // Return item has been collected from the customer.
    public void markReturnCollected() {
        this.status = OrderStatus.RETURN_COLLECTED;
    }

    // Refund kicked off with payment-service — see ReturnService.handleRefundCompleted() for the terminal hop.
    public void initiateRefund() {
        this.status = OrderStatus.REFUND_INITIATED;
    }

    // Payment-service confirmed the refund actually completed — the one true terminal return state.
    public void completeReturn() {
        this.status = OrderStatus.RETURNED;
    }

    // Whether the order is DELIVERED and within its return window, using the real deliveredAt timestamp set by markDelivered() for both physical (agent marks it) and virtual (instant on payment capture) orders — no more approximating from placedAt.
    public boolean isReturnable() {
        boolean withinWindow = deliveredAt != null && deliveredAt.isAfter(Instant.now().minus(java.time.Duration.ofDays(2)));
        return status == OrderStatus.DELIVERED && withinWindow;
    }

    // This order's id.
    public UUID getId() {
        return id;
    }

    // Human-readable order number.
    public String getOrderNumber() {
        return orderNumber;
    }

    // The customer who placed the order.
    public UUID getUserId() {
        return userId;
    }

    // Current order status.
    public OrderStatus getStatus() {
        return status;
    }

    // Currency of the order's monetary amounts.
    public String getCurrency() {
        return currency;
    }

    // Sum of line-item totals before fees/tax.
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    // Shipping fee charged.
    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    // Packaging fee charged.
    public BigDecimal getPackagingFee() {
        return packagingFee;
    }

    // Tax charged.
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    // Final total charged for the order.
    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    // Portion of the total paid from wallet balance.
    public BigDecimal getWalletAmountUsed() {
        return walletAmountUsed;
    }

    // Payment method used.
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    // When the order was placed.
    public Instant getPlacedAt() {
        return placedAt;
    }

    // When the order was cancelled/returned, if applicable.
    public Instant getCancelledAt() {
        return cancelledAt;
    }

    // Physical vs virtual fulfillment.
    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    // The store fulfilling this order's physical items.
    public UUID getStoreId() {
        return storeId;
    }

    // Id of the delivery agent who claimed this order.
    public UUID getDeliveryAgentId() {
        return deliveryAgentId;
    }

    // Snapshotted name of the delivery agent.
    public String getDeliveryAgentName() {
        return deliveryAgentName;
    }

    // When the order was delivered.
    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    // Id of the agent who claimed this order's return pickup.
    public UUID getReturnAgentId() {
        return returnAgentId;
    }

    // Snapshotted name of the return-pickup agent.
    public String getReturnAgentName() {
        return returnAgentName;
    }

    // When an admin reviewed this order's cancellation/return case.
    public Instant getAdminReviewedAt() {
        return adminReviewedAt;
    }

    // Id of the admin who reviewed the case.
    public UUID getAdminReviewedByUserId() {
        return adminReviewedByUserId;
    }

    // Admin's review note.
    public String getAdminNote() {
        return adminNote;
    }

    // Reason given for cancellation/return.
    public String getCancelReason() {
        return cancelReason;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Order order)) return false;
        return id != null && Objects.equals(id, order.id);
    }

    // Hash consistent with id-based equality.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
