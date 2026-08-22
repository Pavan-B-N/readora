package com.readora.commerce.dto;

import java.util.UUID;

public record PaymentCapturedEvent(UUID orderId, UUID paymentId) {
}
