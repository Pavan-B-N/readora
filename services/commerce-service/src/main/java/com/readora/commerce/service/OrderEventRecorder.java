package com.readora.commerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.OrderStatusChangedEvent;
import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import com.readora.commerce.entity.OrderStatusHistory;
import com.readora.commerce.entity.OutboxEvent;
import com.readora.commerce.kafka.KafkaTopics;
import com.readora.commerce.repository.OrderStatusHistoryRepository;
import com.readora.commerce.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The one piece every order-lifecycle service (checkout, fulfillment, returns) shares: recording
 * a status transition and publishing events through the transactional outbox. Split out on its
 * own so those services depend on "how do I record a transition" rather than each duplicating
 * history/outbox plumbing — see the OrderService split this replaced.
 */
@Component
public class OrderEventRecorder {

    private final OrderStatusHistoryRepository historyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderEventRecorder(
            OrderStatusHistoryRepository historyRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.historyRepository = historyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /** Records the transition and publishes order.status_changed — the single source powering the notification feed. */
    public void recordHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, String reason, String changedBy) {
        historyRepository.save(new OrderStatusHistory(order, fromStatus, toStatus, reason, changedBy));
        publish("Order", order.getId(), KafkaTopics.ORDER_STATUS_CHANGED, new OrderStatusChangedEvent(
                order.getId(), order.getUserId(), order.getOrderNumber(), toStatus.name(),
                order.getDeliveryType().name(), order.getStoreId()
        ));
    }

    public void publish(String aggregateType, UUID aggregateId, String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEvent(aggregateType, aggregateId, topic, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event payload", e);
        }
    }
}
