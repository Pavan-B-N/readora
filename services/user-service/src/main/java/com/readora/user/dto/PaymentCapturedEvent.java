package com.readora.user.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCapturedEvent(UUID orderId, UUID paymentId, UUID userId, BigDecimal amount, BigDecimal walletAmountUsed) {
}
