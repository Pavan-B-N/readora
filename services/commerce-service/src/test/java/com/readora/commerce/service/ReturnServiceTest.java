package com.readora.commerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.ReturnOrderRequest;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.PaymentMethod;
import com.readora.commerce.entity.ReturnSenderRole;
import com.readora.commerce.exception.InvalidReturnTransitionException;
import com.readora.commerce.exception.OrderAlreadyCancelledException;
import com.readora.commerce.exception.OrderAlreadyShippedException;
import com.readora.commerce.exception.OrderCancelWindowExpiredException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.exception.OrderNotReturnableException;
import com.readora.commerce.exception.ReturnNotUnderReviewException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private ReturnMessageService returnMessageService;

    private ReturnService returnService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        OrderEventRecorder eventRecorder = new OrderEventRecorder(historyRepository, outboxEventRepository, new ObjectMapper());
        returnService = new ReturnService(orderRepository, userServiceClient, returnMessageService, eventRecorder);
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

    // ---- cancel ----

    @Test
    void cancel_notFound_throws() {
        when(orderRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> returnService.cancel(userId, UUID.randomUUID(), new CancelOrderRequest("changed mind")))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancel_alreadyCancelled_throws() {
        Order order = newOrder(OrderStatus.CANCELLED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.cancel(userId, order.getId(), new CancelOrderRequest("x")))
                .isInstanceOf(OrderAlreadyCancelledException.class);
    }

    @Test
    void cancel_alreadyShipped_throws() {
        Order order = newOrder(OrderStatus.SHIPPED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.cancel(userId, order.getId(), new CancelOrderRequest("x")))
                .isInstanceOf(OrderAlreadyShippedException.class);
    }

    @Test
    void cancel_windowExpired_throws() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "placedAt", Instant.now().minus(Duration.ofHours(49)));
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.cancel(userId, order.getId(), new CancelOrderRequest("x")))
                .isInstanceOf(OrderCancelWindowExpiredException.class);
    }

    @Test
    void cancel_valid_marksCancelledAndPublishesEvent() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        var response = returnService.cancel(userId, order.getId(), new CancelOrderRequest("changed mind"));

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(historyRepository).save(any());
        verify(outboxEventRepository, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    // ---- returnOrder ----

    @Test
    void returnOrder_notReturnable_throws() {
        Order order = newOrder(OrderStatus.CONFIRMED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.returnOrder(userId, order.getId(), new ReturnOrderRequest("defective")))
                .isInstanceOf(OrderNotReturnableException.class);
    }

    @Test
    void returnOrder_virtual_autoInitiatesRefundWithoutAdminNotification() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.VIRTUAL);
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofHours(1)));
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        returnService.returnOrder(userId, order.getId(), new ReturnOrderRequest("not what I wanted"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_INITIATED);
        verify(userServiceClient, never()).getAdminUserIdForStore(any());
    }

    @Test
    void returnOrder_physical_notifiesStoreAdminAndStaysAtRequested() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofHours(1)));
        UUID storeId = UUID.randomUUID();
        order.setStoreId(storeId);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(userServiceClient.getAdminUserIdForStore(storeId)).thenReturn(UUID.randomUUID());

        returnService.returnOrder(userId, order.getId(), new ReturnOrderRequest("damaged"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    @Test
    void returnOrder_physical_noAdminAssignedToStore_isANoOp() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "deliveredAt", Instant.now().minus(Duration.ofHours(1)));
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(userServiceClient.getAdminUserIdForStore(any())).thenReturn(null);

        returnService.returnOrder(userId, order.getId(), new ReturnOrderRequest("damaged"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
    }

    // ---- reviewReturn ----

    @Test
    void reviewReturn_notUnderReview_throws() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "APPROVE", "ok"))
                .isInstanceOf(ReturnNotUnderReviewException.class);
    }

    @Test
    void reviewReturn_approve_transitionsToApproved() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        returnService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "approve", "looks good");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_APPROVED);
    }

    @Test
    void reviewReturn_reject_transitionsToRejected() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        returnService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "REJECT", "no receipt");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REJECTED);
    }

    @Test
    void reviewReturn_invalidDecision_throws() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        UUID storeId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.reviewReturn(UUID.randomUUID(), order.getId(), storeId, "MAYBE", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- updateReturnPickupStatus ----

    @Test
    void updateReturnPickupStatus_illegalTransition_throws() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> returnService.updateReturnPickupStatus(
                order.getId(), OrderStatus.RETURN_COLLECTED, UUID.randomUUID(), "Agent"))
                .isInstanceOf(InvalidReturnTransitionException.class);
    }

    @Test
    void updateReturnPickupStatus_enRoute_transitions() {
        Order order = newOrder(OrderStatus.RETURN_ASSIGNED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        returnService.updateReturnPickupStatus(order.getId(), OrderStatus.RETURN_EN_ROUTE, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_EN_ROUTE);
    }

    @Test
    void updateReturnPickupStatus_collected_initiatesRefund() {
        Order order = newOrder(OrderStatus.RETURN_EN_ROUTE, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        returnService.updateReturnPickupStatus(order.getId(), OrderStatus.RETURN_COLLECTED, UUID.randomUUID(), "Agent");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_INITIATED);
    }

    @Test
    void updateReturnPickupStatus_assigned_setsAgentSnapshot() {
        Order order = newOrder(OrderStatus.RETURN_APPROVED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        returnService.updateReturnPickupStatus(order.getId(), OrderStatus.RETURN_ASSIGNED, UUID.randomUUID(), "Agent Smith");

        assertThat(order.getReturnAgentName()).isEqualTo("Agent Smith");
    }

    // ---- handleRefundCompleted ----

    @Test
    void handleRefundCompleted_wrongStatus_isANoOp() {
        Order order = newOrder(OrderStatus.DELIVERED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        returnService.handleRefundCompleted(order.getId());

        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleRefundCompleted_success_marksReturned() {
        Order order = newOrder(OrderStatus.REFUND_INITIATED, DeliveryType.PHYSICAL);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        returnService.handleRefundCompleted(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURNED);
    }

    @Test
    void handleRefundCompleted_orderNotFound_isANoOp() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        returnService.handleRefundCompleted(UUID.randomUUID());

        verify(orderRepository, never()).save(any());
    }

    // ---- return messages ----

    @Test
    void listReturnMessages_delegatesToReturnMessageService() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));
        when(returnMessageService.list(order.getId())).thenReturn(List.of());

        assertThat(returnService.listReturnMessages(userId, order.getId())).isEmpty();
    }

    @Test
    void postReturnMessage_delegatesToReturnMessageServiceAsCustomer() {
        Order order = newOrder(OrderStatus.RETURN_REQUESTED, DeliveryType.PHYSICAL);
        when(orderRepository.findByIdAndUserId(order.getId(), userId)).thenReturn(Optional.of(order));

        returnService.postReturnMessage(userId, order.getId(), "Any update?");

        verify(returnMessageService).post(order, userId, ReturnSenderRole.CUSTOMER, "Any update?");
    }

    @Test
    void postReturnMessage_orderNotFound_throws() {
        when(orderRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> returnService.postReturnMessage(userId, UUID.randomUUID(), "hi"))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
