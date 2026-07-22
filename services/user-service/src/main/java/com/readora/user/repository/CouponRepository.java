package com.readora.user.repository;

import com.readora.user.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Spring Data repository for coupons.
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    // Case-insensitive lookup by coupon code.
    Optional<Coupon> findByCodeIgnoreCase(String code);
}
