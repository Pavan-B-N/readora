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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// One provider call made while trying to settle a Payment; a Payment can have several across retries.
@Entity
@Table(name = "payment_attempts", schema = "payments")
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "provider_response", columnDefinition = "text")
    private String providerResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // No-arg constructor required by JPA; not for application use.
    protected PaymentAttempt() {
    }

    // Records one attempt outcome for the given payment.
    public PaymentAttempt(Payment payment, int attemptNo, PaymentStatus status, String providerResponse) {
        this.payment = payment;
        this.attemptNo = attemptNo;
        this.status = status;
        this.providerResponse = providerResponse;
    }

    // Stamps the creation timestamp before the row is first persisted.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Primary key.
    public UUID getId() {
        return id;
    }

    // The payment this attempt belongs to.
    public Payment getPayment() {
        return payment;
    }

    // Ordinal of this attempt among the payment's retries.
    public int getAttemptNo() {
        return attemptNo;
    }

    // Outcome status of this attempt.
    public PaymentStatus getStatus() {
        return status;
    }

    // Id-based equality, and only for persisted entities.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PaymentAttempt that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Id-based hash code, consistent with equals().
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
