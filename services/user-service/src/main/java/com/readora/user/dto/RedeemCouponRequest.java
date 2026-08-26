package com.readora.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemCouponRequest(@NotBlank String code) {
}
