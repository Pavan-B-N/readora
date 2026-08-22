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

/** A placed order. No wallet fields yet — wallet-funded checkout is deferred. */
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

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "grand_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal grandTotal;

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

    protected Order() {
    }

    public Order(
            String orderNumber, UUID userId, String currency, BigDecimal subtotal,
            BigDecimal shippingFee, BigDecimal taxAmount, BigDecimal grandTotal, String idempotencyKey,
            DeliveryType deliveryType
    ) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.currency = currency;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.taxAmount = taxAmount;
        this.grandTotal = grandTotal;
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

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancelReason = reason;
    }

    public boolean isCancellable() {
        boolean withinWindow = placedAt.isAfter(Instant.now().minus(java.time.Duration.ofHours(48)));
        boolean notShippedOrBeyond = status != OrderStatus.SHIPPED
                && status != OrderStatus.DELIVERED
                && status != OrderStatus.CANCELLED;
        return withinWindow && notShippedOrBeyond;
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

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
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
