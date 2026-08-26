package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A wallet-credit coupon code — redeem-once-per-user, Amazon-Pay-style. */
@Entity
@Table(name = "coupons", schema = "users")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Coupon() {
    }

    public boolean isRedeemable() {
        return active && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Coupon coupon)) return false;
        return id != null && Objects.equals(id, coupon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
