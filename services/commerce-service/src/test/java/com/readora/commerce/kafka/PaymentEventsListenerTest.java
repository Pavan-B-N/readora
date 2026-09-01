package com.readora.commerce.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.PaymentCapturedEvent;
import com.readora.commerce.dto.PaymentFailedEvent;
import com.readora.commerce.dto.RefundCompletedEvent;
import com.readora.commerce.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventsListenerTest {

    @Mock private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PaymentEventsListener listener;

    @Test
    void onPaymentCaptured_delegatesToOrderService() throws Exception {
        listener = new PaymentEventsListener(orderService, objectMapper);
        UUID orderId = UUID.randomUUID();
        PaymentCapturedEvent event = new PaymentCapturedEvent(orderId, UUID.randomUUID());

        listener.onPaymentCaptured(objectMapper.writeValueAsString(event));

        verify(orderService).handlePaymentCaptured(orderId);
    }

    @Test
    void onPaymentFailed_delegatesToOrderService() throws Exception {
        listener = new PaymentEventsListener(orderService, objectMapper);
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(orderId, "CARD_DECLINED", "card declined");

        listener.onPaymentFailed(objectMapper.writeValueAsString(event));

        verify(orderService).handlePaymentFailed(orderId);
    }

    @Test
    void onRefundCompleted_delegatesToOrderService() throws Exception {
        listener = new PaymentEventsListener(orderService, objectMapper);
        UUID orderId = UUID.randomUUID();
        RefundCompletedEvent event = new RefundCompletedEvent(orderId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), new BigDecimal("50.00"));

        listener.onRefundCompleted(objectMapper.writeValueAsString(event));

        verify(orderService).handleRefundCompleted(orderId);
    }
}
