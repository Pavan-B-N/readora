package com.readora.commerce.service;

import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.exception.InvalidDeliveryTransitionException;
import com.readora.commerce.exception.OrderNotFoundException;
import com.readora.commerce.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// The forward happy path once an order exists: payment settling (PaymentEventsListener) through physical delivery progression (InternalDeliveryController, delivery-agent-service) — distinct from CheckoutService (creates the order), OrderQueryService (reads it), and ReturnService (reverses it after the fact).
@Service
public class OrderFulfillmentService {

    private final OrderRepository orderRepository;
    private final OrderEventRecorder eventRecorder;

    // Wires the order repository and event recorder.
    public OrderFulfillmentService(OrderRepository orderRepository, OrderEventRecorder eventRecorder) {
        this.orderRepository = orderRepository;
        this.eventRecorder = eventRecorder;
    }

    // Advances a paid order through PAID -> CONFIRMED; physical orders stop at CONFIRMED (SHIPPED/DELIVERED await a future shipping integration), while virtual orders go straight on to DELIVERED since there's nothing to ship.
    @Transactional
    public void handlePaymentCaptured(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.transitionTo(OrderStatus.PAID);
        orderRepository.save(order);
        eventRecorder.recordHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, null, "system");

        order.transitionTo(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        eventRecorder.recordHistory(order, OrderStatus.PAID, OrderStatus.CONFIRMED, null, "system");

        if (order.getDeliveryType() == DeliveryType.VIRTUAL) {
            order.markDelivered();
            orderRepository.save(order);
            eventRecorder.recordHistory(order, OrderStatus.CONFIRMED, OrderStatus.DELIVERED, null, "system");
        }
    }

    // Moves a still-pending order to PAYMENT_FAILED.
    @Transactional
    public void handlePaymentFailed(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        order.transitionTo(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        eventRecorder.recordHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED, null, "system");
    }

    // Called by delivery-agent-service (via InternalDeliveryController) as an agent progresses a physical order; enforces CONFIRMED -> ASSIGNED -> SHIPPED -> DELIVERED in order, no skipping a step.
    @Transactional
    public void updateDeliveryStatus(UUID orderId, OrderStatus newStatus, UUID deliveryAgentId, String deliveryAgentName) {
        Order order = orderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);
        OrderStatus previousStatus = order.getStatus();

        boolean legal = switch (newStatus) {
            case ASSIGNED -> previousStatus == OrderStatus.CONFIRMED;
            case SHIPPED -> previousStatus == OrderStatus.ASSIGNED;
            case DELIVERED -> previousStatus == OrderStatus.SHIPPED;
            default -> false;
        };
        if (!legal) {
            throw new InvalidDeliveryTransitionException();
        }

        switch (newStatus) {
            case ASSIGNED -> order.assignToAgent(deliveryAgentId, deliveryAgentName);
            case SHIPPED -> order.markOutForDelivery();
            case DELIVERED -> order.markDelivered();
            default -> throw new InvalidDeliveryTransitionException();
        }
        orderRepository.save(order);
        eventRecorder.recordHistory(order, previousStatus, newStatus, null, "delivery-agent");
    }
}
