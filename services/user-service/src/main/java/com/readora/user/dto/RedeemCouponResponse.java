package com.readora.user.dto;

import java.math.BigDecimal;

public record RedeemCouponResponse(BigDecimal creditedAmount, BigDecimal balance, String currency) {
}
