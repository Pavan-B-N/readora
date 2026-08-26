package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Tracks that a user already redeemed a given coupon — one redemption per user per coupon. */
@Entity
@Table(name = "coupon_redemptions", schema = "users")
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt;

    protected CouponRedemption() {
    }

    public CouponRedemption(UUID couponId, UUID userId) {
        this.couponId = couponId;
        this.userId = userId;
    }

    @PrePersist
    protected void onCreate() {
        this.redeemedAt = Instant.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CouponRedemption that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
