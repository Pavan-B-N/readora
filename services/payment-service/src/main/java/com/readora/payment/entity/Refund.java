package com.readora.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// A refund issued against a captured Payment.
@Entity
@Table(name = "refunds", schema = "payments")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "reason")
    private String reason;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // No-arg constructor required by JPA; not for application use.
    protected Refund() {
    }

    // Creates a pending refund for the given payment.
    public Refund(Payment payment, BigDecimal amount, String reason) {
        this.payment = payment;
        this.amount = amount;
        this.reason = reason;
    }

    // Stamps the creation timestamp before the row is first persisted.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Transitions the refund to COMPLETED and records when.
    public void complete() {
        this.status = RefundStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    // Transitions the refund to FAILED.
    public void fail() {
        this.status = RefundStatus.FAILED;
    }

    // Primary key.
    public UUID getId() {
        return id;
    }

    // The payment this refund is against.
    public Payment getPayment() {
        return payment;
    }

    // Refunded amount.
    public BigDecimal getAmount() {
        return amount;
    }

    // Current lifecycle status.
    public RefundStatus getStatus() {
        return status;
    }

    // When the refund completed, or null if not yet.
    public Instant getCompletedAt() {
        return completedAt;
    }

    // Id-based equality, and only for persisted entities.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Refund refund)) return false;
        return id != null && Objects.equals(id, refund.id);
    }

    // Id-based hash code, consistent with equals().
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
