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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Component
public class OrderEventsListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    /**
     * Payout scales with order value and item count instead of a flat rate — base fee, plus up to
     * a capped bonus for higher-value orders (proxy for distance/risk, since this build has no
     * geo-distance data), plus a small per-extra-item handling bonus. A pickup pays a bit less
     * than a full delivery since it's the simpler leg.
     */
    private static final BigDecimal DELIVERY_BASE_PAYOUT = new BigDecimal("25.00");
    private static final BigDecimal PICKUP_BASE_PAYOUT = new BigDecimal("20.00");
    private static final BigDecimal VALUE_BONUS_RATE = new BigDecimal("0.05");
    private static final BigDecimal VALUE_BONUS_CAP = new BigDecimal("40.00");
    private static final BigDecimal ITEM_BONUS_PER_EXTRA = new BigDecimal("3.00");
    private static final BigDecimal ITEM_BONUS_CAP = new BigDecimal("15.00");

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
                snapshot.recipientName(), snapshot.recipientPhone(), snapshot.itemsJson(),
                computePayout(DELIVERY_BASE_PAYOUT, snapshot.grandTotal(), snapshot.itemCount())
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
                snapshot.recipientName(), snapshot.recipientPhone(), snapshot.itemsJson(),
                computePayout(PICKUP_BASE_PAYOUT, snapshot.grandTotal(), snapshot.itemCount())
        ));
    }

    private record DeliveryDetailSnapshot(
            String destinationCity, String recipientName, String recipientPhone, String itemsJson,
            BigDecimal grandTotal, int itemCount
    ) {
        static final DeliveryDetailSnapshot EMPTY = new DeliveryDetailSnapshot(null, null, null, null, null, 0);
    }

    private BigDecimal computePayout(BigDecimal basePayout, BigDecimal grandTotal, int itemCount) {
        BigDecimal valueBonus = grandTotal == null ? BigDecimal.ZERO : grandTotal.multiply(VALUE_BONUS_RATE).min(VALUE_BONUS_CAP);
        BigDecimal itemBonus = ITEM_BONUS_PER_EXTRA.multiply(BigDecimal.valueOf(Math.max(0, itemCount - 1))).min(ITEM_BONUS_CAP);
        return basePayout.add(valueBonus).add(itemBonus).setScale(2, RoundingMode.HALF_UP);
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
            List<OrderDeliveryDetailResponse.Item> items = detail.items();
            String itemsJson = items == null || items.isEmpty() ? null : objectMapper.writeValueAsString(items);
            int itemCount = items == null ? 0 : items.stream().mapToInt(OrderDeliveryDetailResponse.Item::qty).sum();
            return new DeliveryDetailSnapshot(city, recipientName, recipientPhone, itemsJson, detail.grandTotal(), itemCount);
        } catch (Exception e) {
            log.warn("Could not resolve delivery detail snapshot for order {}", orderId, e);
            return DeliveryDetailSnapshot.EMPTY;
        }
    }
}
