package com.readora.sharedcore.event;

import java.util.UUID;

/** Consumed by commerce-service to mark an order's payment as failed. */
public record PaymentFailedEvent(UUID orderId, String failureCode, String failureReason) {
}
