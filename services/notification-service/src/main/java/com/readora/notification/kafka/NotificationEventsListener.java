package com.readora.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.notification.dto.OrderCancelledEvent;
import com.readora.notification.dto.PaymentCapturedEvent;
import com.readora.notification.dto.PaymentFailedEvent;
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

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "notification-service")
    public void onOrderCancelled(String payload) throws Exception {
        OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
        notificationService.push(event.userId(), "ORDER_CANCELLED", event);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_CAPTURED, groupId = "notification-service")
    public void onPaymentCaptured(String payload) throws Exception {
        PaymentCapturedEvent event = objectMapper.readValue(payload, PaymentCapturedEvent.class);
        notificationService.push(event.userId(), "ORDER_CONFIRMED", event);
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "notification-service")
    public void onPaymentFailed(String payload) throws Exception {
        PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
        notificationService.push(event.userId(), "PAYMENT_FAILED", event);
    }

    @KafkaListener(topics = KafkaTopics.REFUND_COMPLETED, groupId = "notification-service")
    public void onRefundCompleted(String payload) throws Exception {
        RefundCompletedEvent event = objectMapper.readValue(payload, RefundCompletedEvent.class);
        notificationService.push(event.userId(), "REFUND_ISSUED", event);
    }
}
