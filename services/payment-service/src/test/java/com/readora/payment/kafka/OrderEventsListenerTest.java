package com.readora.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.sharedcore.event.OrderCancelledEvent;
import com.readora.sharedcore.event.OrderCreatedEvent;
import com.readora.sharedcore.event.OrderReturnedEvent;
import com.readora.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventsListenerTest {

    @Mock private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OrderEventsListener listener;

    @Test
    void onOrderCreated_delegatesToPaymentService() throws Exception {
        listener = new OrderEventsListener(paymentService, objectMapper);
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), List.of(),
                new BigDecimal("500.00"), new BigDecimal("500.00"), "WALLET");

        listener.onOrderCreated(objectMapper.writeValueAsString(event));

        verify(paymentService).handleOrderCreated(eq(event));
    }

    @Test
    void onOrderCancelled_delegatesToPaymentService() throws Exception {
        listener = new OrderEventsListener(paymentService, objectMapper);
        OrderCancelledEvent event = new OrderCancelledEvent(UUID.randomUUID(), UUID.randomUUID(), "changed mind", new BigDecimal("500.00"));

        listener.onOrderCancelled(objectMapper.writeValueAsString(event));

        verify(paymentService).handleOrderCancelled(eq(event));
    }

    @Test
    void onOrderReturned_delegatesToPaymentService() throws Exception {
        listener = new OrderEventsListener(paymentService, objectMapper);
        OrderReturnedEvent event = new OrderReturnedEvent(UUID.randomUUID(), UUID.randomUUID(), "damaged", new BigDecimal("500.00"));

        listener.onOrderReturned(objectMapper.writeValueAsString(event));

        verify(paymentService).handleOrderReturned(eq(event));
    }
}
