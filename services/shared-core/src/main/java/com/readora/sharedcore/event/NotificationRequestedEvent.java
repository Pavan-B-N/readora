package com.readora.sharedcore.event;

import java.util.UUID;

// A generic, arbitrarily-targeted notification (unlike OrderStatusChangedEvent, userId can be anyone, e.g. a store admin) with title/message pre-rendered here rather than derived on the consumer side.
public record NotificationRequestedEvent(UUID userId, String type, String title, String message, UUID orderId) {
}
