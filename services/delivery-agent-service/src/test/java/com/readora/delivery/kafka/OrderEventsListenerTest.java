package com.readora.delivery.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.dto.OrderDeliveryDetailResponse;
import com.readora.sharedcore.event.OrderStatusChangedEvent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventsListenerTest {

    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private ReturnPickupAssignmentRepository returnPickupRepository;
    @Mock private CommerceClient commerceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OrderEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderEventsListener(assignmentRepository, returnPickupRepository, commerceClient, objectMapper);
    }

    private OrderDeliveryDetailResponse detail(BigDecimal grandTotal, int itemCount) {
        List<OrderDeliveryDetailResponse.Item> items = itemCount == 0
                ? List.of()
                : List.of(new OrderDeliveryDetailResponse.Item("Clean Code", itemCount));
        return new OrderDeliveryDetailResponse(
                UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", UUID.randomUUID(),
                new OrderDeliveryDetailResponse.ShippingAddress("Ravi Kumar", "L1", null, "Bengaluru", "KA", "560001", "IN", "999"),
                items, grandTotal, Instant.now()
        );
    }

    @Test
    void onOrderStatusChanged_confirmedPhysicalOrder_createsAssignmentWithComputedPayout() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", "PHYSICAL", storeId);
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(commerceClient.getDeliveryDetail(orderId)).thenReturn(detail(new BigDecimal("500.00"), 3));

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        ArgumentCaptor<DeliveryAssignment> captor = ArgumentCaptor.forClass(DeliveryAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        // base 25.00 + min(500*0.05, 40) = 25 + 25 = 50, + min(3-1)*3=6 -> 56.00
        assertThat(captor.getValue().getPayoutAmount()).isEqualByComparingTo("56.00");
    }

    @Test
    void onOrderStatusChanged_valueBonusIsCapped() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", "PHYSICAL", UUID.randomUUID());
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(commerceClient.getDeliveryDetail(orderId)).thenReturn(detail(new BigDecimal("5000.00"), 1));

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        ArgumentCaptor<DeliveryAssignment> captor = ArgumentCaptor.forClass(DeliveryAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        // base 25 + cap 40 + 0 item bonus (only 1 item) = 65.00
        assertThat(captor.getValue().getPayoutAmount()).isEqualByComparingTo("65.00");
    }

    @Test
    void onOrderStatusChanged_virtualOrder_isIgnored() throws Exception {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", "VIRTUAL", UUID.randomUUID());

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void onOrderStatusChanged_notConfirmedStatus_isIgnored() throws Exception {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "RDA-2026-000001", "PAID", "PHYSICAL", UUID.randomUUID());

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void onOrderStatusChanged_confirmedWithNoStoreId_skipsAssignment() throws Exception {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", "PHYSICAL", null);

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void onOrderStatusChanged_alreadyHasAssignment_isIdempotent() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", "PHYSICAL", UUID.randomUUID());
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(org.mockito.Mockito.mock(DeliveryAssignment.class)));

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(assignmentRepository, never()).save(any());
        verify(commerceClient, never()).getDeliveryDetail(any());
    }

    @Test
    void onOrderStatusChanged_commerceClientUnreachable_degradesToEmptySnapshotRatherThanFailing() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, UUID.randomUUID(), "RDA-2026-000001", "CONFIRMED", "PHYSICAL", UUID.randomUUID());
        when(assignmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(commerceClient.getDeliveryDetail(orderId)).thenThrow(new RuntimeException("unreachable"));

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        ArgumentCaptor<DeliveryAssignment> captor = ArgumentCaptor.forClass(DeliveryAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getDestinationCity()).isNull();
        assertThat(captor.getValue().getPayoutAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void onOrderStatusChanged_returnApproved_createsPickupAssignment() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, UUID.randomUUID(), "RDA-2026-000001", "RETURN_APPROVED", "PHYSICAL", storeId);
        when(returnPickupRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(commerceClient.getDeliveryDetail(orderId)).thenReturn(detail(new BigDecimal("500.00"), 1));

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(returnPickupRepository).save(any(ReturnPickupAssignment.class));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void onOrderStatusChanged_returnApprovedWithNoStoreId_skipsPickup() throws Exception {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "RDA-2026-000001", "RETURN_APPROVED", "PHYSICAL", null);

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(returnPickupRepository, never()).save(any());
    }

    @Test
    void onOrderStatusChanged_returnApprovedAlreadyHasPickup_isIdempotent() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, UUID.randomUUID(), "RDA-2026-000001", "RETURN_APPROVED", "PHYSICAL", UUID.randomUUID());
        when(returnPickupRepository.findByOrderId(orderId)).thenReturn(Optional.of(org.mockito.Mockito.mock(ReturnPickupAssignment.class)));

        listener.onOrderStatusChanged(objectMapper.writeValueAsString(event));

        verify(returnPickupRepository, never()).save(any());
    }
}
