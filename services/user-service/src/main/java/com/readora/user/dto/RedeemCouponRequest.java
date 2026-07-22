package com.readora.user.dto;

import jakarta.validation.constraints.NotBlank;

// Request body for redeeming a coupon code.
public record RedeemCouponRequest(@NotBlank String code) {
}
