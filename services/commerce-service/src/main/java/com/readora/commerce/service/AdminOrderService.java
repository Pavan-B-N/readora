package com.readora.commerce.service;

import com.readora.commerce.client.PaymentClient;
import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.AdminOrderSummaryResponse;
import com.readora.commerce.dto.RefundStatus;
import com.readora.commerce.dto.ReturnMessageResponse;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.ReturnSenderRole;
import com.readora.commerce.exception.AdminOrderNotFoundException;
import com.readora.commerce.exception.AdminStoreNotAssignedException;
import com.readora.commerce.repository.OrderRepository;
import com.readora.sharedcore.security.CurrentUserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Store-scoped visibility into cancelled/returned orders and their refund status, plus a lightweight "mark reviewed with a note" action — see Order.markReviewed's comment for why that's the extent of "acting on it" given refunds are fully automatic in this build.
@Service
public class AdminOrderService {

    private static final List<OrderStatus> RETURN_STATUSES = List.of(
            OrderStatus.CANCELLED,
            OrderStatus.RETURN_REQUESTED,
            OrderStatus.RETURN_REJECTED,
            OrderStatus.RETURN_APPROVED,
            OrderStatus.RETURN_ASSIGNED,
            OrderStatus.RETURN_EN_ROUTE,
            OrderStatus.RETURN_COLLECTED,
            OrderStatus.REFUND_INITIATED,
            OrderStatus.RETURNED
    );

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final PaymentClient paymentClient;
    private final ReturnService returnService;
    private final ReturnMessageService returnMessageService;

    // Wires the order repository, user-service and payment clients, and the return services.
    public AdminOrderService(
            OrderRepository orderRepository,
            UserServiceClient userServiceClient,
            PaymentClient paymentClient,
            ReturnService returnService,
            ReturnMessageService returnMessageService
    ) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
        this.paymentClient = paymentClient;
        this.returnService = returnService;
        this.returnMessageService = returnMessageService;
    }

    // Resolves the calling admin's assigned store, or rejects if they have none.
    private UUID resolveCallerStoreId() {
        UUID storeId = userServiceClient.getAdminStoreId(CurrentUserContext.require());
        if (storeId == null) {
            throw new AdminStoreNotAssignedException();
        }
        return storeId;
    }

    // Lists every cancelled/returned order for the caller's store, enriched with refund status.
    @Transactional(readOnly = true)
    public Page<AdminOrderSummaryResponse> listReturns(Pageable pageable) {
        UUID storeId = resolveCallerStoreId();
        Page<Order> orders = orderRepository.findAllByStoreIdAndStatusInOrderByPlacedAtDesc(storeId, RETURN_STATUSES, pageable);

        List<UUID> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<UUID, RefundStatus> refundStatuses = paymentClient.getRefundStatuses(orderIds);

        return orders.map(order -> toSummary(order, refundStatuses.get(order.getId())));
    }

    // Lists orders needing admin attention (RETURN_REQUESTED to decide, CANCELLED to optionally note), excluding any row that already has adminReviewedAt set.
    @Transactional(readOnly = true)
    public Page<AdminOrderSummaryResponse> listPendingReturns(Pageable pageable) {
        UUID storeId = resolveCallerStoreId();
        List<OrderStatus> pendingStatuses = List.of(OrderStatus.CANCELLED, OrderStatus.RETURN_REQUESTED);
        Page<Order> orders = orderRepository.findAllByStoreIdAndStatusInAndAdminReviewedAtIsNullOrderByPlacedAtDesc(
                storeId, pendingStatuses, pageable);

        List<UUID> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<UUID, RefundStatus> refundStatuses = paymentClient.getRefundStatuses(orderIds);

        return orders.map(order -> toSummary(order, refundStatuses.get(order.getId())));
    }

    // Lists all return/cancellation statuses an admin has already recorded a note/decision on, including terminal outcomes (RETURNED, RETURN_REJECTED) and in-progress returns (RETURN_APPROVED, RETURN_ASSIGNED, etc.) acknowledged before the pickup flow began.
    @Transactional(readOnly = true)
    public Page<AdminOrderSummaryResponse> listReviewedReturns(Pageable pageable) {
        UUID storeId = resolveCallerStoreId();
        Page<Order> orders = orderRepository.findAllByStoreIdAndStatusInAndAdminReviewedAtIsNotNullOrderByPlacedAtDesc(
                storeId, RETURN_STATUSES, pageable);

        List<UUID> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<UUID, RefundStatus> refundStatuses = paymentClient.getRefundStatuses(orderIds);

        return orders.map(order -> toSummary(order, refundStatuses.get(order.getId())));
    }

    // One return/cancellation case's full detail — backs the dedicated review page (not the list row).
    @Transactional(readOnly = true)
    public AdminOrderSummaryResponse getReturn(UUID orderId) {
        UUID storeId = resolveCallerStoreId();
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(AdminOrderNotFoundException::new);
        RefundStatus refundStatus = paymentClient.getRefundStatuses(List.of(orderId)).get(orderId);
        return toSummary(order, refundStatus);
    }

    // Records an admin's review: decision is "APPROVE"/"REJECT" for a return awaiting review, or null for a plain cancellation note (nothing to decide there — see ReviewOrderRequest's comment).
    @Transactional
    public AdminOrderSummaryResponse reviewOrder(UUID orderId, String note, String decision) {
        UUID storeId = resolveCallerStoreId();
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(AdminOrderNotFoundException::new);

        if (decision != null && !decision.isBlank()) {
            returnService.reviewReturn(CurrentUserContext.require(), orderId, storeId, decision, note);
            order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(AdminOrderNotFoundException::new);
        } else {
            order.markReviewed(CurrentUserContext.require(), note);
            orderRepository.save(order);
        }

        RefundStatus refundStatus = paymentClient.getRefundStatuses(List.of(orderId)).get(orderId);
        return toSummary(order, refundStatus);
    }

    // Lists a return's chat thread, scoped to the caller's store.
    @Transactional(readOnly = true)
    public List<ReturnMessageResponse> listReturnMessages(UUID orderId) {
        UUID storeId = resolveCallerStoreId();
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(AdminOrderNotFoundException::new);
        return returnMessageService.list(order.getId());
    }

    // Posts a return-chat message as the admin, scoped to the caller's store.
    @Transactional
    public ReturnMessageResponse postReturnMessage(UUID orderId, String content) {
        UUID storeId = resolveCallerStoreId();
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(AdminOrderNotFoundException::new);
        return returnMessageService.post(order, CurrentUserContext.require(), ReturnSenderRole.ADMIN, content);
    }

    // Maps an order and its (possibly absent) refund status to the admin summary DTO.
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
