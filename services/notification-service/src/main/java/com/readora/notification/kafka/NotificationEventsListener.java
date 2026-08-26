package com.readora.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.notification.dto.OrderStatusChangedEvent;
import com.readora.notification.dto.RefundCompletedEvent;
import com.readora.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventsListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public NotificationEventsListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Single source for every order-lifecycle notification (placed, paid, confirmed, shipped,
     * delivered, cancelled, payment failed) — commerce-service publishes this on every status
     * transition, so this listener covers the whole order journey generically rather than
     * needing a bespoke Kafka listener per status.
     */
    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED, groupId = "notification-service")
    public void onOrderStatusChanged(String payload) throws Exception {
        OrderStatusChangedEvent event = objectMapper.readValue(payload, OrderStatusChangedEvent.class);
        String[] text = textFor(event.toStatus(), event.orderNumber());
        notificationService.create(event.userId(), "ORDER_" + event.toStatus(), text[0], text[1], event.orderId());
    }

    @KafkaListener(topics = KafkaTopics.REFUND_COMPLETED, groupId = "notification-service")
    public void onRefundCompleted(String payload) throws Exception {
        RefundCompletedEvent event = objectMapper.readValue(payload, RefundCompletedEvent.class);
        notificationService.create(
                event.userId(), "REFUND_ISSUED", "Refund processed",
                "₹" + event.amount() + " was refunded to your wallet.", event.orderId()
        );
    }

    private String[] textFor(String status, String orderNumber) {
        return switch (status) {
            case "PENDING_PAYMENT" -> new String[]{"Order placed", "Your order " + orderNumber + " has been placed."};
            case "PAID" -> new String[]{"Payment received", "We've received payment for order " + orderNumber + "."};
            case "CONFIRMED" -> new String[]{"Order confirmed", "Order " + orderNumber + " is confirmed and being prepared."};
            case "SHIPPED" -> new String[]{"Order shipped", "Order " + orderNumber + " is on its way."};
            case "DELIVERED" -> new String[]{"Order delivered", "Order " + orderNumber + " has been delivered."};
            case "CANCELLED" -> new String[]{"Order cancelled", "Order " + orderNumber + " was cancelled."};
            case "PAYMENT_FAILED" -> new String[]{
                    "Payment failed", "Payment for order " + orderNumber + " failed. Check your wallet balance and try again."
            };
            default -> new String[]{"Order updated", "Order " + orderNumber + " is now " + status + "."};
        };
    }
}
