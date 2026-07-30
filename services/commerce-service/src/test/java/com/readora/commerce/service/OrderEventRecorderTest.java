package com.readora.commerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.PaymentMethod;
import com.readora.commerce.repository.OrderStatusHistoryRepository;
import com.readora.commerce.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventRecorderTest {

    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private OutboxEventRepository outboxEventRepository;

    private OrderEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new OrderEventRecorder(historyRepository, outboxEventRepository, new ObjectMapper());
    }

    private static Order order() {
        Order order = new Order(
                "RDA-2026-000001", UUID.randomUUID(), "INR", new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("9.00"), new BigDecimal("109.00"),
                BigDecimal.ZERO, PaymentMethod.WALLET, UUID.randomUUID().toString(), DeliveryType.VIRTUAL
        );
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        return order;
    }

    @Test
    void recordHistory_savesHistoryRowAndPublishesStatusChangedEvent() {
        Order order = order();

        recorder.recordHistory(order, null, OrderStatus.PENDING_PAYMENT, "reason", "system");

        verify(historyRepository).save(any());
        verify(outboxEventRepository).save(any());
    }

    @Test
    void publish_serializesPayloadAndSavesToOutbox() {
        recorder.publish("Order", UUID.randomUUID(), "order.created", java.util.Map.of("orderId", "abc"));

        verify(outboxEventRepository).save(any());
    }

    @Test
    void publish_unserializablePayload_wrapsInIllegalStateException() {
        // A self-referencing structure Jackson can't serialize (infinite recursion) — the
        // simplest way to force writeValueAsString to fail without a broken ObjectMapper.
        class SelfReferencing {
            @SuppressWarnings("unused")
            public SelfReferencing self = this;
        }

        assertThatThrownBy(() -> recorder.publish("Order", UUID.randomUUID(), "order.created", new SelfReferencing()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void recordHistory_publishesUnderTheOrderStatusChangedTopic() {
        Order order = order();

        recorder.recordHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, null, "system");

        verify(outboxEventRepository).save(org.mockito.ArgumentMatchers.argThat(
                event -> event.getEventType().equals("order.status_changed")
        ));
    }
}
