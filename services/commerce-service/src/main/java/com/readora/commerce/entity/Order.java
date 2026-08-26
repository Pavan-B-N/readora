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

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

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

    /**
     * Snapshot of whichever delivery agent claimed this order — never a live lookup into
     * delivery-agent-service, same reasoning as OrderItem's title/isbn snapshots.
     */
    @Column(name = "delivery_agent_id")
    private UUID deliveryAgentId;

    @Column(name = "delivery_agent_name")
    private String deliveryAgentName;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /**
     * Lets a store admin acknowledge a cancellation/return case with a note — the "way to act on
     * it" for return/refund visibility, since refunds themselves are fully automatic (dummy
     * payment provider, no manual approval gate) and there's no separate dispute-ticket entity.
     */
    @Column(name = "admin_reviewed_at")
    private Instant adminReviewedAt;

    @Column(name = "admin_reviewed_by_user_id")
    private UUID adminReviewedByUserId;

    @Column(name = "admin_note", columnDefinition = "text")
    private String adminNote;

    protected Order() {
    }

    public Order(
            String orderNumber, UUID userId, String currency, BigDecimal subtotal,
            BigDecimal shippingFee, BigDecimal packagingFee, BigDecimal taxAmount, BigDecimal grandTotal,
            BigDecimal walletAmountUsed, String paymentMethod, String idempotencyKey, DeliveryType deliveryType
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

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.placedAt = Instant.now();
    }

    public void transitionTo(OrderStatus newStatus) {
        this.status = newStatus;
    }

    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    /** A delivery agent has claimed this physical order. */
    public void assignToAgent(UUID agentId, String agentName) {
        this.status = OrderStatus.ASSIGNED;
        this.deliveryAgentId = agentId;
        this.deliveryAgentName = agentName;
    }

    /** Out for delivery — kept as the SHIPPED status, see OrderStatus's javadoc. */
    public void markOutForDelivery() {
        this.status = OrderStatus.SHIPPED;
    }

    public void markDelivered() {
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public void markReviewed(UUID reviewerId, String note) {
        this.adminReviewedAt = Instant.now();
        this.adminReviewedByUserId = reviewerId;
        this.adminNote = note;
    }

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelReason = reason;
    }

    public boolean isCancellable() {
        boolean withinWindow = placedAt.isAfter(Instant.now().minus(java.time.Duration.ofHours(48)));
        boolean notAssignedOrBeyond = status != OrderStatus.ASSIGNED
                && status != OrderStatus.SHIPPED
                && status != OrderStatus.DELIVERED
                && status != OrderStatus.CANCELLED;
        return withinWindow && notAssignedOrBeyond;
    }

    public void returnOrder(String reason) {
        this.status = OrderStatus.RETURNED;
        this.cancelledAt = Instant.now();
        this.cancelReason = reason;
    }

    /**
     * Uses the real deliveredAt timestamp, set by markDelivered() for both physical (agent marks
     * it) and virtual (instant on payment capture) orders — no more approximating from placedAt.
     */
    public boolean isReturnable() {
        boolean withinWindow = deliveredAt != null && deliveredAt.isAfter(Instant.now().minus(java.time.Duration.ofDays(7)));
        return status == OrderStatus.DELIVERED && withinWindow;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getPackagingFee() {
        return packagingFee;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public BigDecimal getWalletAmountUsed() {
        return walletAmountUsed;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public UUID getDeliveryAgentId() {
        return deliveryAgentId;
    }

    public String getDeliveryAgentName() {
        return deliveryAgentName;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getAdminReviewedAt() {
        return adminReviewedAt;
    }

    public UUID getAdminReviewedByUserId() {
        return adminReviewedByUserId;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Order order)) return false;
        return id != null && Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
