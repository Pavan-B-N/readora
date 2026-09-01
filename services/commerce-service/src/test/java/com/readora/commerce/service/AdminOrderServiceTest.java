package com.readora.commerce.service;

import com.readora.commerce.client.PaymentClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.exception.AdminOrderNotFoundException;
import com.readora.commerce.exception.AdminStoreNotAssignedException;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderService orderService;
    @Mock private ReturnMessageService returnMessageService;

    private AdminOrderService adminOrderService;
    private final UUID adminId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adminOrderService = new AdminOrderService(orderRepository, userServiceClient, paymentClient, orderService, returnMessageService);
        CurrentUserContext.set(adminId, List.of("ADMIN"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private static Order order(OrderStatus status) {
        Order order = new Order("RDA-2026-000001", UUID.randomUUID(), "INR", new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("9.00"), new BigDecimal("109.00"), BigDecimal.ZERO, "WALLET",
                UUID.randomUUID().toString(), DeliveryType.PHYSICAL);
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(order, "placedAt", Instant.now());
        order.transitionTo(status);
        return order;
    }

    @Test
    void listReturns_noStoreAssigned_throws() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(null);

        assertThatThrownBy(() -> adminOrderService.listReturns(Pageable.unpaged()))
                .isInstanceOf(AdminStoreNotAssignedException.class);
    }

    @Test
    void listReturns_mapsOrdersWithRefundStatuses() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        Order order = order(OrderStatus.RETURNED);
        when(orderRepository.findAllByStoreIdAndStatusInOrderByPlacedAtDesc(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(paymentClient.getRefundStatuses(any())).thenReturn(Map.of());

        var page = adminOrderService.listReturns(Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listPendingReturns_delegatesToPendingQuery() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        when(orderRepository.findAllByStoreIdAndStatusInAndAdminReviewedAtIsNullOrderByPlacedAtDesc(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(paymentClient.getRefundStatuses(any())).thenReturn(Map.of());

        adminOrderService.listPendingReturns(Pageable.unpaged());

        verify(orderRepository).findAllByStoreIdAndStatusInAndAdminReviewedAtIsNullOrderByPlacedAtDesc(any(), any(), any());
    }

    @Test
    void listReviewedReturns_delegatesToReviewedQuery() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        when(orderRepository.findAllByStoreIdAndStatusInAndAdminReviewedAtIsNotNullOrderByPlacedAtDesc(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(paymentClient.getRefundStatuses(any())).thenReturn(Map.of());

        adminOrderService.listReviewedReturns(Pageable.unpaged());

        verify(orderRepository).findAllByStoreIdAndStatusInAndAdminReviewedAtIsNotNullOrderByPlacedAtDesc(any(), any(), any());
    }

    @Test
    void getReturn_notFound_throws() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        when(orderRepository.findByIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.getReturn(UUID.randomUUID()))
                .isInstanceOf(AdminOrderNotFoundException.class);
    }

    @Test
    void getReturn_found_returnsSummary() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        Order order = order(OrderStatus.RETURN_REQUESTED);
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));
        when(paymentClient.getRefundStatuses(any())).thenReturn(Map.of());

        var response = adminOrderService.getReturn(order.getId());

        assertThat(response.orderId()).isEqualTo(order.getId());
    }

    @Test
    void reviewOrder_withDecision_delegatesToOrderServiceReviewReturn() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        Order order = order(OrderStatus.RETURN_APPROVED);
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));
        when(paymentClient.getRefundStatuses(any())).thenReturn(Map.of());

        adminOrderService.reviewOrder(order.getId(), "looks good", "APPROVE");

        verify(orderService).reviewReturn(adminId, order.getId(), storeId, "APPROVE", "looks good");
    }

    @Test
    void reviewOrder_withoutDecision_justMarksReviewed() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        Order order = order(OrderStatus.CANCELLED);
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));
        when(paymentClient.getRefundStatuses(any())).thenReturn(Map.of());

        adminOrderService.reviewOrder(order.getId(), "acknowledged", null);

        assertThat(order.getAdminNote()).isEqualTo("acknowledged");
        verify(orderRepository).save(order);
        verify(orderService, org.mockito.Mockito.never()).reviewReturn(any(), any(), any(), any(), any());
    }

    @Test
    void listReturnMessages_notFound_throws() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        when(orderRepository.findByIdAndStoreId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.listReturnMessages(UUID.randomUUID()))
                .isInstanceOf(AdminOrderNotFoundException.class);
    }

    @Test
    void postReturnMessage_delegatesToReturnMessageServiceAsAdmin() {
        when(userServiceClient.getAdminStoreId(adminId)).thenReturn(storeId);
        Order order = order(OrderStatus.RETURN_REQUESTED);
        when(orderRepository.findByIdAndStoreId(order.getId(), storeId)).thenReturn(Optional.of(order));

        adminOrderService.postReturnMessage(order.getId(), "We're reviewing this");

        verify(returnMessageService).post(order, adminId, com.readora.commerce.entity.ReturnSenderRole.ADMIN, "We're reviewing this");
    }
}
