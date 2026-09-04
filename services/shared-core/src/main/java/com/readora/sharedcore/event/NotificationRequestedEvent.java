package com.readora.sharedcore.event;

import java.util.UUID;

/**
 * A generic, arbitrarily-targeted notification for notification-service to create — unlike
 * OrderStatusChangedEvent (always the order's own customer), userId here can be anyone, e.g. a
 * store's admin. title/message are pre-rendered here rather than derived from a status enum on
 * the consumer side, since notification-service has no reason to know about return-chat or
 * admin-review semantics.
 */
public record NotificationRequestedEvent(UUID userId, String type, String title, String message, UUID orderId) {
}
