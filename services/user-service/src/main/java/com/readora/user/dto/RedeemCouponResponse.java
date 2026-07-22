package com.readora.user.dto;

import java.math.BigDecimal;

// Response after redeeming a coupon.
public record RedeemCouponResponse(BigDecimal creditedAmount, BigDecimal balance, String currency) {
}
