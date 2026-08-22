package com.readora.commerce.dto;

import java.util.UUID;

public record PaymentFailedEvent(UUID orderId, String failureCode, String failureReason) {
}
