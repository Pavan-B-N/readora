package com.readora.commerce.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private Order newOrder(DeliveryType deliveryType) {
        return new Order(
                "ORD-1", UUID.randomUUID(), "INR", new BigDecimal("100.00"), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("100.00"),
                BigDecimal.ZERO, PaymentMethod.WALLET, UUID.randomUUID().toString(), deliveryType
        );
    }

    @Test
    void isReturnable_deliveredWithinTwoDays_isTrue() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        order.markDelivered();
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofHours(36)));

        assertThat(order.isReturnable()).isTrue();
    }

    @Test
    void isReturnable_deliveredMoreThanTwoDaysAgo_isFalse() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        order.markDelivered();
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofDays(3)));

        assertThat(order.isReturnable()).isFalse();
    }

    @Test
    void isReturnable_neverDelivered_isFalse() {
        Order order = newOrder(DeliveryType.PHYSICAL);

        assertThat(order.isReturnable()).isFalse();
    }

    @Test
    void isReturnable_alreadyInReturnFlow_isFalse() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        order.markDelivered();
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofDays(1)));
        order.requestReturn("changed my mind");

        assertThat(order.isReturnable()).isFalse();
    }

    @Test
    void isCancellable_freshPendingOrder_isTrue() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "placedAt", Instant.now());

        assertThat(order.isCancellable()).isTrue();
    }

    @Test
    void isCancellable_outsideFortyEightHourWindow_isFalse() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "placedAt", Instant.now().minus(Duration.ofHours(49)));

        assertThat(order.isCancellable()).isFalse();
    }

    @Test
    void isCancellable_onceAssignedToAgent_isFalseEvenWithinWindow() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "placedAt", Instant.now());
        order.assignToAgent(UUID.randomUUID(), "Agent Smith");

        assertThat(order.isCancellable()).isFalse();
    }

    @Test
    void returnWorkflow_walksThroughEveryStatusInOrder() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        order.markDelivered();
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now());

        order.requestReturn("damaged in transit");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);

        order.approveReturn();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_APPROVED);

        UUID agentId = UUID.randomUUID();
        order.assignReturnAgent(agentId, "Pickup Agent");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_ASSIGNED);
        assertThat(order.getReturnAgentId()).isEqualTo(agentId);

        order.markReturnEnRoute();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_EN_ROUTE);

        order.markReturnCollected();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_COLLECTED);

        order.initiateRefund();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_INITIATED);

        order.completeReturn();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    @Test
    void rejectReturn_isTerminalAndDoesNotTouchReturnAgentFields() {
        Order order = newOrder(DeliveryType.PHYSICAL);
        order.markDelivered();
        order.requestReturn("no longer needed");

        order.rejectReturn();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REJECTED);
        assertThat(order.getReturnAgentId()).isNull();
    }
}
