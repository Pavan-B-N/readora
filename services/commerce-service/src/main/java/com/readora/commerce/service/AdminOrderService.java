package com.readora.commerce.service;

import com.readora.commerce.client.PaymentClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.AdminOrderSummaryResponse;
import com.readora.commerce.dto.RefundStatus;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.exception.AdminOrderNotFoundException;
import com.readora.commerce.exception.AdminStoreNotAssignedException;
import com.readora.commerce.repository.OrderRepository;
import com.readora.commerce.security.CurrentUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Store-scoped visibility into cancelled/returned orders and their refund status, plus a
 * lightweight "mark reviewed with a note" action — see Order.markReviewed's javadoc for why
 * that's the extent of "acting on it" given refunds are fully automatic in this build.
 */
@Service
public class AdminOrderService {

    private static final List<OrderStatus> RETURN_STATUSES = List.of(OrderStatus.CANCELLED, OrderStatus.RETURNED);

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final PaymentClient paymentClient;

    public AdminOrderService(OrderRepository orderRepository, UserServiceClient userServiceClient, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
        this.paymentClient = paymentClient;
    }

    private UUID resolveCallerStoreId() {
        UUID storeId = userServiceClient.getAdminStoreId(CurrentUserContext.require());
        if (storeId == null) {
            throw new AdminStoreNotAssignedException();
        }
        return storeId;
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderSummaryResponse> listReturns(Pageable pageable) {
        UUID storeId = resolveCallerStoreId();
        Page<Order> orders = orderRepository.findAllByStoreIdAndStatusInOrderByPlacedAtDesc(storeId, RETURN_STATUSES, pageable);

        List<UUID> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<UUID, RefundStatus> refundStatuses = paymentClient.getRefundStatuses(orderIds);

        return orders.map(order -> toSummary(order, refundStatuses.get(order.getId())));
    }

    @Transactional
    public AdminOrderSummaryResponse reviewOrder(UUID orderId, String note) {
        UUID storeId = resolveCallerStoreId();
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(AdminOrderNotFoundException::new);

        order.markReviewed(CurrentUserContext.require(), note);
        orderRepository.save(order);

        RefundStatus refundStatus = paymentClient.getRefundStatuses(List.of(orderId)).get(orderId);
        return toSummary(order, refundStatus);
    }

    private AdminOrderSummaryResponse toSummary(Order order, RefundStatus refundStatus) {
        return new AdminOrderSummaryResponse(
                order.getId(), order.getOrderNumber(), order.getStatus().name(), order.getGrandTotal(), order.getCurrency(),
                order.getPlacedAt(), order.getCancelledAt(), order.getCancelReason(),
                refundStatus != null ? refundStatus.status() : null,
                refundStatus != null ? refundStatus.amount() : null,
                refundStatus != null ? refundStatus.completedAt() : null,
                order.getAdminReviewedAt(), order.getAdminNote()
        );
    }
}
