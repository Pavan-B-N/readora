package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Current wallet balance for one user — a derived cache of the WalletTransaction ledger. */
@Entity
@Table(name = "wallet_accounts", schema = "users")
public class WalletAccount {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA no-arg constructor.
    protected WalletAccount() {
    }

    // Creates a new zero-balance wallet for a user.
    public WalletAccount(UUID userId) {
        this.userId = userId;
    }

    // Stamps the updated timestamp before update.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Adds to the balance.
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    // Subtracts from the balance.
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    // Wallet owner's user id.
    public UUID getUserId() {
        return userId;
    }

    // Current balance.
    public BigDecimal getBalance() {
        return balance;
    }

    // Currency code.
    public String getCurrency() {
        return currency;
    }

    // When the balance was last updated.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Entity equality by user id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WalletAccount that)) return false;
        return userId != null && Objects.equals(userId, that.userId);
    }

    // Entity hash by user id.
    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
