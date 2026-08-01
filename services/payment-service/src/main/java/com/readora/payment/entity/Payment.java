package com.readora.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** order_id/user_id are cross-service references (commerce-service / auth-service own those rows) — plain UUID columns, never a JPA relationship, since those entities live in a different service/schema entirely. */
@Entity
@Table(name = "payments", schema = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "wallet_amount_used", precision = 10, scale = 2)
    private BigDecimal walletAmountUsed = BigDecimal.ZERO;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // No-arg constructor required by JPA; not for application use.
    protected Payment() {
    }

    // Creates a new payment in the initial (default) status for the given order.
    public Payment(UUID orderId, UUID userId, PaymentMethod method, BigDecimal amount, String idempotencyKey) {
        this.orderId = orderId;
        this.userId = userId;
        this.method = method;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }

    // Stamps created/updated timestamps before the row is first persisted.
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Refreshes the updated timestamp before any change is persisted.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Transitions the payment to AUTHORIZED and records when.
    public void authorize() {
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = Instant.now();
    }

    /** Set once, at authorization time, from the checkout's verified wallet-funded amount. */
    public void useWallet(BigDecimal amount) {
        this.walletAmountUsed = amount;
    }

    // Transitions the payment to CAPTURED and records when.
    public void capture() {
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = Instant.now();
    }

    // Transitions the payment to FAILED with the given failure code and reason.
    public void fail(String code, String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = code;
        this.failureReason = reason;
    }

    // Transitions the payment to REFUNDED.
    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    // Primary key.
    public UUID getId() {
        return id;
    }

    // The cross-service order id this payment is for.
    public UUID getOrderId() {
        return orderId;
    }

    // The cross-service user id this payment belongs to.
    public UUID getUserId() {
        return userId;
    }

    // How this payment was funded.
    public PaymentMethod getMethod() {
        return method;
    }

    // Current lifecycle status.
    public PaymentStatus getStatus() {
        return status;
    }

    // Total payment amount.
    public BigDecimal getAmount() {
        return amount;
    }

    // Portion of the amount funded from the user's wallet.
    public BigDecimal getWalletAmountUsed() {
        return walletAmountUsed;
    }

    // Client-supplied key used to dedupe repeated payment attempts.
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    // When the payment was authorized, or null if not yet.
    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    // When the payment was captured, or null if not yet.
    public Instant getCapturedAt() {
        return capturedAt;
    }

    // Machine-readable failure code, set only if the payment failed.
    public String getFailureCode() {
        return failureCode;
    }

    // Human-readable failure reason, set only if the payment failed.
    public String getFailureReason() {
        return failureReason;
    }

    // When this payment record was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Id-based equality, and only for persisted entities.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Payment payment)) return false;
        return id != null && Objects.equals(id, payment.id);
    }

    // Id-based hash code, consistent with equals().
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
