package com.readora.delivery.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.dto.OrderDeliveryDetailResponse;
import com.readora.delivery.dto.OrderStatusChangedEvent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final DeliveryAssignmentRepository assignmentRepository;
    private final ReturnPickupAssignmentRepository returnPickupRepository;
    private final CommerceClient commerceClient;
    private final ObjectMapper objectMapper;

    public OrderEventsListener(
            DeliveryAssignmentRepository assignmentRepository,
            ReturnPickupAssignmentRepository returnPickupRepository,
            CommerceClient commerceClient,
            ObjectMapper objectMapper
    ) {
        this.assignmentRepository = assignmentRepository;
        this.returnPickupRepository = returnPickupRepository;
        this.commerceClient = commerceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * A physical order just reached CONFIRMED — queue it for a delivery agent to claim, or its
     * return was just approved — queue a pickup for one instead. Ignores everything else (virtual
     * orders, and every other status transition an order goes through). Idempotent: findByOrderId
     * guards against the same event being processed twice.
     */
    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED, groupId = "delivery-agent-service")
    public void onOrderStatusChanged(String payload) throws Exception {
        OrderStatusChangedEvent event = objectMapper.readValue(payload, OrderStatusChangedEvent.class);

        if ("RETURN_APPROVED".equals(event.toStatus())) {
            onReturnApproved(event);
            return;
        }

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

        DeliveryDetailSnapshot snapshot = resolveSnapshot(event.orderId());
        assignmentRepository.save(new DeliveryAssignment(
                event.orderId(), event.orderNumber(), event.storeId(), snapshot.destinationCity(),
                snapshot.recipientName(), snapshot.recipientPhone(), snapshot.itemsJson()
        ));
    }

    private void onReturnApproved(OrderStatusChangedEvent event) {
        if (event.storeId() == null) {
            log.warn("Order {}'s return was approved with no storeId — skipping pickup assignment", event.orderNumber());
            return;
        }
        if (returnPickupRepository.findByOrderId(event.orderId()).isPresent()) {
            return;
        }

        DeliveryDetailSnapshot snapshot = resolveSnapshot(event.orderId());
        returnPickupRepository.save(new ReturnPickupAssignment(
                event.orderId(), event.orderNumber(), event.storeId(), snapshot.destinationCity(),
                snapshot.recipientName(), snapshot.recipientPhone(), snapshot.itemsJson()
        ));
    }

    private record DeliveryDetailSnapshot(String destinationCity, String recipientName, String recipientPhone, String itemsJson) {
        static final DeliveryDetailSnapshot EMPTY = new DeliveryDetailSnapshot(null, null, null, null);
    }

    /**
     * Best-effort, resolved once here rather than on every queue read — a missing value is a
     * cosmetic gap (the agent just won't see it until they open the detail page, which fetches
     * commerce-service live), not worth failing assignment creation over if it's briefly down.
     */
    private DeliveryDetailSnapshot resolveSnapshot(UUID orderId) {
        try {
            OrderDeliveryDetailResponse detail = commerceClient.getDeliveryDetail(orderId);
            String city = detail.shippingAddress() != null ? detail.shippingAddress().city() : null;
            String recipientName = detail.shippingAddress() != null ? detail.shippingAddress().recipientName() : null;
            String recipientPhone = detail.shippingAddress() != null ? detail.shippingAddress().phone() : null;
            // Field names match OrderDeliveryDetailResponse.Item exactly, so this round-trips
            // cleanly through ItemSnapshot on the read side with no manual mapping.
            String itemsJson = detail.items() == null || detail.items().isEmpty()
                    ? null
                    : objectMapper.writeValueAsString(detail.items());
            return new DeliveryDetailSnapshot(city, recipientName, recipientPhone, itemsJson);
        } catch (Exception e) {
            log.warn("Could not resolve delivery detail snapshot for order {}", orderId, e);
            return DeliveryDetailSnapshot.EMPTY;
        }
    }
}
