package com.readora.user.repository;

import com.readora.user.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    boolean existsByCouponIdAndUserId(UUID couponId, UUID userId);
}
