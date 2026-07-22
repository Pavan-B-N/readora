package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One append-only wallet ledger entry — a reversal is a new row, never an edit. */
@Entity
@Table(name = "wallet_transactions", schema = "users")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WalletTransactionType type;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA no-arg constructor.
    protected WalletTransaction() {
    }

    // Creates a new ledger entry.
    public WalletTransaction(
            UUID userId, UUID orderId, BigDecimal amount, WalletTransactionType type,
            BigDecimal balanceAfter, String idempotencyKey
    ) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.type = type;
        this.balanceAfter = balanceAfter;
        this.idempotencyKey = idempotencyKey;
    }

    // Stamps the created timestamp before insert.
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Transaction id.
    public UUID getId() {
        return id;
    }

    // Related order id, if any.
    public UUID getOrderId() {
        return orderId;
    }

    // Transaction amount.
    public BigDecimal getAmount() {
        return amount;
    }

    // Transaction type.
    public WalletTransactionType getType() {
        return type;
    }

    // Wallet balance immediately after this transaction.
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    // When the transaction was created.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WalletTransaction that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Entity hash by id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
