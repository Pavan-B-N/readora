package com.readora.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.sharedcore.event.OrderCancelledEvent;
import com.readora.sharedcore.event.OrderCreatedEvent;
import com.readora.sharedcore.event.OrderReturnedEvent;
import com.readora.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// Consumes order lifecycle events from Kafka and dispatches them to PaymentService.
@Component
public class OrderEventsListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    // Wires the service and mapper used to handle incoming events.
    public OrderEventsListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    // Deserializes and forwards an order-created event to initiate payment.
    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "payment-service")
    public void onOrderCreated(String payload) throws Exception {
        paymentService.handleOrderCreated(objectMapper.readValue(payload, OrderCreatedEvent.class));
    }

    // Deserializes and forwards an order-cancelled event to void/reverse the payment.
    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "payment-service")
    public void onOrderCancelled(String payload) throws Exception {
        paymentService.handleOrderCancelled(objectMapper.readValue(payload, OrderCancelledEvent.class));
    }

    // Deserializes and forwards an order-returned event to trigger a refund.
    @KafkaListener(topics = KafkaTopics.ORDER_RETURNED, groupId = "payment-service")
    public void onOrderReturned(String payload) throws Exception {
        paymentService.handleOrderReturned(objectMapper.readValue(payload, OrderReturnedEvent.class));
    }
}
