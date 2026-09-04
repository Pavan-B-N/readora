package com.readora.commerce.service;

import com.readora.commerce.client.UserServiceClient;
import com.readora.commerce.dto.CancelOrderRequest;
import com.readora.commerce.dto.CancelOrderResponse;
import com.readora.sharedcore.event.NotificationRequestedEvent;
import com.readora.sharedcore.event.OrderCancelledEvent;
import com.readora.sharedcore.event.OrderReturnedEvent;
import com.readora.commerce.dto.ReturnMessageResponse;
import com.readora.commerce.dto.ReturnOrderRequest;
import com.readora.commerce.dto.ReturnOrderResponse;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.ReturnSenderRole;
import com.readora.commerce.exception.InvalidReturnTransitionException;
import com.readora.commerce.exception.OrderAlreadyCancelledException;
import com.readora.commerce.exception.OrderAlreadyShippedException;
import com.readora.commerce.exception.OrderCancelWindowExpiredException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.exception.OrderNotReturnableException;
import com.readora.commerce.exception.ReturnNotUnderReviewException;
import com.readora.commerce.kafka.KafkaTopics;
import com.readora.commerce.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every way an order can end early and get refunded — cancellation before it ships, and the full
 * return lifecycle (request, admin review, pickup, refund) after it's delivered. Split out of the
 * original monolithic OrderService: distinct from CheckoutService (creates the order),
 * OrderQueryService (reads it), and OrderFulfillmentService (the forward path this reverses).
 */
@Service
public class ReturnService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ReturnMessageService returnMessageService;
    private final OrderEventRecorder eventRecorder;

    public ReturnService(
            OrderRepository orderRepository,
            UserServiceClient userServiceClient,
            ReturnMessageService returnMessageService,
            OrderEventRecorder eventRecorder
    ) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
        this.returnMessageService = returnMessageService;
        this.eventRecorder = eventRecorder;
    }

    @Transactional
    public CancelOrderResponse cancel(UUID userId, UUID orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderAlreadyCancelledException();
        }
        if (order.getStatus() == OrderStatus.ASSIGNED
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderAlreadyShippedException();
        }
        if (!order.isCancellable()) {
            throw new OrderCancelWindowExpiredException();
        }

        OrderStatus previousStatus = order.getStatus();
        order.cancel(request.reason());
        orderRepository.save(order);

        eventRecorder.recordHistory(order, previousStatus, OrderStatus.CANCELLED, request.reason(), "user");

        eventRecorder.publish("Order", order.getId(), KafkaTopics.ORDER_CANCELLED,
                new OrderCancelledEvent(order.getId(), userId, request.reason(), order.getGrandTotal()));

        return new CancelOrderResponse(order.getId(), order.getStatus().name(), order.getCancelledAt());
    }

    /**
     * A virtual-only order has nothing physical to inspect, so it auto-advances straight through
     * to REFUND_INITIATED in this same call — no admin review, no chat. An order with a physical
     * item stops at RETURN_REQUESTED and waits for an admin decision (see reviewReturn()); the
     * refund itself doesn't fire until the book is actually collected
     * (updateReturnPickupStatus() -> RETURN_COLLECTED).
     */
    @Transactional
    public ReturnOrderResponse returnOrder(UUID userId, UUID orderId, ReturnOrderRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);

        if (!order.isReturnable()) {
            throw new OrderNotReturnableException();
        }

        OrderStatus previousStatus = order.getStatus();
        order.requestReturn(request.reason());
        orderRepository.save(order);
        eventRecorder.recordHistory(order, previousStatus, OrderStatus.RETURN_REQUESTED, request.reason(), "user");

        if (order.getDeliveryType() == DeliveryType.VIRTUAL) {
            initiateRefund(order, request.reason(), "system");
        } else {
            notifyAdminOfReturnRequest(order);
        }

        return new ReturnOrderResponse(order.getId(), order.getStatus().name(), order.getCancelledAt());
    }

    /** Only physical returns actually need an admin's attention — a virtual one auto-approves above and never waits on review. */
    private void notifyAdminOfReturnRequest(Order order) {
        UUID adminUserId = userServiceClient.getAdminUserIdForStore(order.getStoreId());
        if (adminUserId == null) {
            return;
        }
        eventRecorder.publish("Order", order.getId(), KafkaTopics.NOTIFICATION_REQUESTED, new NotificationRequestedEvent(
                adminUserId, "RETURN_REQUESTED_ADMIN", "New return to review",
                "Order " + order.getOrderNumber() + " has a return awaiting your review.", order.getId()
        ));
    }

    /**
     * The admin decision gate for a return with a physical item (a virtual-only return never
     * reaches RETURN_REQUESTED for long enough to be reviewed — see returnOrder()). Approving
     * queues a pickup for delivery-agent-service (it listens for RETURN_APPROVED on
     * order.status_changed); rejecting is terminal, no refund.
     */
    @Transactional
    public void reviewReturn(UUID adminId, UUID orderId, UUID storeId, String decision, String note) {
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId).orElseThrow(OrderNotFoundException::new);
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new ReturnNotUnderReviewException();
        }

        order.markReviewed(adminId, note);

        OrderStatus previousStatus = order.getStatus();
        if ("APPROVE".equalsIgnoreCase(decision)) {
            order.approveReturn();
            orderRepository.save(order);
            eventRecorder.recordHistory(order, previousStatus, OrderStatus.RETURN_APPROVED, note, "admin");
        } else if ("REJECT".equalsIgnoreCase(decision)) {
            order.rejectReturn();
            orderRepository.save(order);
            eventRecorder.recordHistory(order, previousStatus, OrderStatus.RETURN_REJECTED, note, "admin");
        } else {
            throw new IllegalArgumentException("decision must be APPROVE or REJECT");
        }
    }

    /**
     * Called by delivery-agent-service (via InternalDeliveryController) as an agent progresses a
     * return pickup. Enforces RETURN_APPROVED -> RETURN_ASSIGNED -> RETURN_EN_ROUTE ->
     * RETURN_COLLECTED in order. Reaching RETURN_COLLECTED immediately kicks off the refund —
     * mirrors OrderFulfillmentService.updateDeliveryStatus()'s shape exactly, just for the
     * reverse (pickup) leg.
     */
    @Transactional
    public void updateReturnPickupStatus(UUID orderId, OrderStatus newStatus, UUID returnAgentId, String returnAgentName) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        OrderStatus previousStatus = order.getStatus();

        boolean legal = switch (newStatus) {
            case RETURN_ASSIGNED -> previousStatus == OrderStatus.RETURN_APPROVED;
            case RETURN_EN_ROUTE -> previousStatus == OrderStatus.RETURN_ASSIGNED;
            case RETURN_COLLECTED -> previousStatus == OrderStatus.RETURN_EN_ROUTE;
            default -> false;
        };
        if (!legal) {
            throw new InvalidReturnTransitionException();
        }

        switch (newStatus) {
            case RETURN_ASSIGNED -> order.assignReturnAgent(returnAgentId, returnAgentName);
            case RETURN_EN_ROUTE -> order.markReturnEnRoute();
            case RETURN_COLLECTED -> order.markReturnCollected();
            default -> throw new InvalidReturnTransitionException();
        }
        orderRepository.save(order);
        eventRecorder.recordHistory(order, previousStatus, newStatus, null, "delivery-agent");

        if (newStatus == OrderStatus.RETURN_COLLECTED) {
            initiateRefund(order, order.getCancelReason(), "system");
        }
    }

    /** Payment-service confirmed the refund actually landed — the one true terminal hop for every return path. */
    @Transactional
    public void handleRefundCompleted(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.REFUND_INITIATED) {
            return;
        }

        OrderStatus previousStatus = order.getStatus();
        order.completeReturn();
        orderRepository.save(order);
        eventRecorder.recordHistory(order, previousStatus, OrderStatus.RETURNED, null, "system");
    }

    /** Shared by both return paths: fires REFUND_INITIATED plus the order.returned event payment-service already consumes unchanged. */
    private void initiateRefund(Order order, String reason, String changedBy) {
        OrderStatus previousStatus = order.getStatus();
        order.initiateRefund();
        orderRepository.save(order);
        eventRecorder.recordHistory(order, previousStatus, OrderStatus.REFUND_INITIATED, reason, changedBy);

        eventRecorder.publish("Order", order.getId(), KafkaTopics.ORDER_RETURNED,
                new OrderReturnedEvent(order.getId(), order.getUserId(), reason, order.getGrandTotal()));
    }

    @Transactional(readOnly = true)
    public List<ReturnMessageResponse> listReturnMessages(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);
        return returnMessageService.list(order.getId());
    }

    @Transactional
    public ReturnMessageResponse postReturnMessage(UUID userId, UUID orderId, String content) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(OrderNotFoundException::new);
        return returnMessageService.post(order, userId, ReturnSenderRole.CUSTOMER, content);
    }
}
