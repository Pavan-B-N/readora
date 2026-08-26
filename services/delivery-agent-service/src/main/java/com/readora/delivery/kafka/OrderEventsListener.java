package com.readora.delivery.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.dto.OrderStatusChangedEvent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final DeliveryAssignmentRepository assignmentRepository;
    private final ObjectMapper objectMapper;

    public OrderEventsListener(DeliveryAssignmentRepository assignmentRepository, ObjectMapper objectMapper) {
        this.assignmentRepository = assignmentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * A physical order just reached CONFIRMED — queue it for a delivery agent to claim. Ignores
     * everything else (virtual orders, and every other status transition an order goes through).
     * Idempotent: findByOrderId guards against the same event being processed twice.
     */
    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED, groupId = "delivery-agent-service")
    public void onOrderStatusChanged(String payload) throws Exception {
        OrderStatusChangedEvent event = objectMapper.readValue(payload, OrderStatusChangedEvent.class);

        if (!"CONFIRMED".equals(event.toStatus()) || !"PHYSICAL".equals(event.deliveryType())) {
            return;
        }
        if (event.storeId() == null) {
            log.warn("Physical order {} reached CONFIRMED with no storeId — skipping delivery assignment", event.orderNumber());
            return;
        }
        if (assignmentRepository.findByOrderId(event.orderId()).isPresent()) {
            return;
        }

        assignmentRepository.save(new DeliveryAssignment(event.orderId(), event.orderNumber(), event.storeId()));
    }
}
