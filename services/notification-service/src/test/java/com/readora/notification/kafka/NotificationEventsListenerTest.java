package com.readora.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.notification.dto.NotificationRequestedEvent;
import com.readora.notification.dto.OrderStatusChangedEvent;
import com.readora.notification.dto.RefundCompletedEvent;
import com.readora.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventsListenerTest {

    @Mock private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NotificationEventsListener listener;

    @Test
    void onOrderStatusChanged_knownStatus_createsNotificationWithMappedText() throws Exception {
        listener = new NotificationEventsListener(notificationService, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, userId, "RDA-2026-000001", "DELIVERED");

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(notificationService).create(eq(userId), eq("ORDER_DELIVERED"), eq("Order delivered"), any(), eq(orderId));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PENDING_PAYMENT", "PAID", "CONFIRMED", "ASSIGNED", "SHIPPED", "CANCELLED", "PAYMENT_FAILED",
            "RETURN_REQUESTED", "RETURN_REJECTED", "RETURN_APPROVED", "RETURN_ASSIGNED", "RETURN_EN_ROUTE",
            "RETURN_COLLECTED", "REFUND_INITIATED", "RETURNED"
    })
    void onOrderStatusChanged_everyKnownStatus_createsANotificationWithoutError(String status) throws Exception {
        listener = new NotificationEventsListener(notificationService, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, userId, "RDA-2026-000001", status);

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(notificationService).create(eq(userId), eq("ORDER_" + status), any(), any(), eq(orderId));
    }

    @Test
    void onOrderStatusChanged_unknownStatus_fallsBackToGenericText() throws Exception {
        listener = new NotificationEventsListener(notificationService, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, userId, "RDA-2026-000001", "SOME_NEW_STATUS");

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(eq(userId), any(), titleCaptor.capture(), any(), eq(orderId));
        assertThat(titleCaptor.getValue()).isEqualTo("Order updated");
    }

    @Test
    void onRefundCompleted_delegatesToNotificationService() throws Exception {
        listener = new NotificationEventsListener(notificationService, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        RefundCompletedEvent event = new RefundCompletedEvent(orderId, UUID.randomUUID(), userId, new BigDecimal("50.00"), new BigDecimal("50.00"));

        listener.onRefundCompleted(objectMapper.writeValueAsString(event));

        verify(notificationService).create(eq(userId), eq("REFUND_ISSUED"), any(), any(), eq(orderId));
    }

    @Test
    void onNotificationRequested_passesThroughSenderProvidedText() throws Exception {
        listener = new NotificationEventsListener(notificationService, objectMapper);
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        NotificationRequestedEvent event = new NotificationRequestedEvent(userId, "RETURN_MESSAGE", "New message", "You have a message", orderId);

        listener.onNotificationRequested(objectMapper.writeValueAsString(event));

        verify(notificationService).create(userId, "RETURN_MESSAGE", "New message", "You have a message", orderId);
    }
}
