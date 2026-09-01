package com.readora.user.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.user.dto.PaymentCapturedEvent;
import com.readora.user.dto.RefundCompletedEvent;
import com.readora.user.service.WalletEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletEventsListenerTest {

    @Mock
    private WalletEventService walletEventService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WalletEventsListener listener;

    @Test
    void onPaymentCaptured_deserializesAndDelegates() throws Exception {
        listener = new WalletEventsListener(walletEventService, objectMapper);
        PaymentCapturedEvent event = new PaymentCapturedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), BigDecimal.ZERO);

        listener.onPaymentCaptured(objectMapper.writeValueAsString(event));

        verify(walletEventService).handlePaymentCaptured(eq(event));
    }

    @Test
    void onRefundCompleted_deserializesAndDelegates() throws Exception {
        listener = new WalletEventsListener(walletEventService, objectMapper);
        RefundCompletedEvent event = new RefundCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), new BigDecimal("50.00"));

        listener.onRefundCompleted(objectMapper.writeValueAsString(event));

        verify(walletEventService).handleRefundCompleted(eq(event));
    }
}
