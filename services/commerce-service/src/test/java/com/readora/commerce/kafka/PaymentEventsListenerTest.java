package com.readora.commerce.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.PaymentCapturedEvent;
import com.readora.commerce.dto.PaymentFailedEvent;
import com.readora.commerce.dto.RefundCompletedEvent;
import com.readora.commerce.service.OrderFulfillmentService;
import com.readora.commerce.service.ReturnService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventsListenerTest {

    @Mock private OrderFulfillmentService orderFulfillmentService;
    @Mock private ReturnService returnService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PaymentEventsListener listener;

    @Test
    void onPaymentCaptured_delegatesToOrderFulfillmentService() throws Exception {
        listener = new PaymentEventsListener(orderFulfillmentService, returnService, objectMapper);
        UUID orderId = UUID.randomUUID();
        PaymentCapturedEvent event = new PaymentCapturedEvent(orderId, UUID.randomUUID());

        listener.onPaymentCaptured(objectMapper.writeValueAsString(event));

        verify(orderFulfillmentService).handlePaymentCaptured(orderId);
    }

    @Test
    void onPaymentFailed_delegatesToOrderFulfillmentService() throws Exception {
        listener = new PaymentEventsListener(orderFulfillmentService, returnService, objectMapper);
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(orderId, "CARD_DECLINED", "card declined");

        listener.onPaymentFailed(objectMapper.writeValueAsString(event));

        verify(orderFulfillmentService).handlePaymentFailed(orderId);
    }

    @Test
    void onRefundCompleted_delegatesToReturnService() throws Exception {
        listener = new PaymentEventsListener(orderFulfillmentService, returnService, objectMapper);
        UUID orderId = UUID.randomUUID();
        RefundCompletedEvent event = new RefundCompletedEvent(orderId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), new BigDecimal("50.00"));

        listener.onRefundCompleted(objectMapper.writeValueAsString(event));

        verify(returnService).handleRefundCompleted(orderId);
    }
}
