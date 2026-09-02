package com.readora.commerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.PaymentMethod;
import com.readora.commerce.exception.InvalidDeliveryTransitionException;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.repository.OrderStatusHistoryRepository;
import com.readora.commerce.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private OutboxEventRepository outboxEventRepository;

    private OrderFulfillmentService fulfillmentService;

    @BeforeEach
    void setUp() {
        OrderEventRecorder eventRecorder = new OrderEventRecorder(historyRepository, outboxEventRepository, new ObjectMapper());
        fulfillmentService = new OrderFulfillmentService(orderRepository, eventRecorder);
    }

    private static Order newOrder(OrderStatus status, DeliveryType deliveryType) {
        Order order = new Order(
                "RDA-2026-000001", UUID.randomUUID(), "INR", new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("9.00"), new BigDecimal("109.00"),
                BigDecimal.ZERO, PaymentMethod.WALLET, UUID.randomUUID().toString(), deliveryType
        );
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(order, "placedAt", Instant.now());
        order.transitionTo(status);
        return order;
    }

    // ---- handlePaymentCaptured ----

    @Test
    void handlePaymentCaptured_wrongStatus_isANoOp() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.handlePaymentCaptured(order.getId());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentCaptured_physical_stopsAtConfirmed() {
        Order order = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.handlePaymentCaptured(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void handlePaymentCaptured_virtual_autoAdvancesToDelivered() {
        Order order = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.VIRTUAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.handlePaymentCaptured(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    // ---- updateDeliveryStatus ----

    @Test
    void updateDeliveryStatus_illegalTransition_throws() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> fulfillmentService.updateDeliveryStatus(order.getId(), OrderStatus.DELIVERED, UUID.randomUUID(), "Agent"))
                .isInstanceOf(InvalidDeliveryTransitionException.class);
    }

    @Test
    void updateDeliveryStatus_assignedFromConfirmed_setsAgentSnapshot() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.updateDeliveryStatus(order.getId(), OrderStatus.ASSIGNED, UUID.randomUUID(), "Agent Smith");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ASSIGNED);
        assertThat(order.getDeliveryAgentName()).isEqualTo("Agent Smith");
    }

    @Test
    void updateDeliveryStatus_shippedFromAssigned_transitions() {
        Order order = newOrder(OrderStatus.ASSIGNED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.updateDeliveryStatus(order.getId(), OrderStatus.SHIPPED, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void updateDeliveryStatus_deliveredFromShipped_marksDeliveredAt() {
        Order order = newOrder(OrderStatus.SHIPPED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.updateDeliveryStatus(order.getId(), OrderStatus.DELIVERED, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    // ---- handlePaymentFailed ----

    @Test
    void handlePaymentFailed_wrongStatus_isANoOp() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.handlePaymentFailed(order.getId());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentFailed_pending_marksFailed() {
        Order order = newOrder(OrderStatus.PENDING_PAYMENT, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        fulfillmentService.handlePaymentFailed(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }
}
