package com.readora.commerce.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.PaymentCapturedEvent;
import com.readora.commerce.dto.PaymentFailedEvent;
import com.readora.commerce.dto.RefundCompletedEvent;
import com.readora.commerce.service.OrderFulfillmentService;
import com.readora.commerce.service.ReturnService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventsListener {

    private final OrderFulfillmentService orderFulfillmentService;
    private final ReturnService returnService;
    private final ObjectMapper objectMapper;

    public PaymentEventsListener(OrderFulfillmentService orderFulfillmentService, ReturnService returnService, ObjectMapper objectMapper) {
        this.orderFulfillmentService = orderFulfillmentService;
        this.returnService = returnService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_CAPTURED, groupId = "commerce-service")
    public void onPaymentCaptured(String payload) throws Exception {
        PaymentCapturedEvent event = objectMapper.readValue(payload, PaymentCapturedEvent.class);
        orderFulfillmentService.handlePaymentCaptured(event.orderId());
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "commerce-service")
    public void onPaymentFailed(String payload) throws Exception {
        PaymentFailedEvent event = objectMapper.readValue(payload, PaymentFailedEvent.class);
        orderFulfillmentService.handlePaymentFailed(event.orderId());
    }

    /** A return's refund actually completed on payment-service's side — the one true terminal hop to RETURNED. */
    @KafkaListener(topics = KafkaTopics.REFUND_COMPLETED, groupId = "commerce-service")
    public void onRefundCompleted(String payload) throws Exception {
        RefundCompletedEvent event = objectMapper.readValue(payload, RefundCompletedEvent.class);
        returnService.handleRefundCompleted(event.orderId());
    }
}
