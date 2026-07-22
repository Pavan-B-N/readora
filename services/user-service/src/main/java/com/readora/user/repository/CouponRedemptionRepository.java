package com.readora.user.repository;

import com.readora.user.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Spring Data repository for coupon redemptions.
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    // Whether a user has already redeemed a given coupon.
    boolean existsByCouponIdAndUserId(UUID couponId, UUID userId);
}
