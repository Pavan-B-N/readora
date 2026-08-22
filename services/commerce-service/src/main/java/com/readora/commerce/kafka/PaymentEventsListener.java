package com.readora.commerce.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.PaymentCapturedEvent;
import com.readora.commerce.dto.PaymentFailedEvent;
import com.readora.commerce.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventsListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentEventsListener(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_CAPTURED, groupId = "commerce-service")
    public void onPaymentCaptured(String payload) throws Exception {
        PaymentCapturedEvent event = objectMapper.readValue(payload, PaymentCapturedEvent.class);
        orderService.handlePaymentCaptured(event.orderId());
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "commerce-service")
    public void onPaymentFailed(String payload) throws Exception {
        PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
        orderService.handlePaymentFailed(event.orderId());
    }
}
