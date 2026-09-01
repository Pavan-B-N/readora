package com.readora.delivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.entity.ReturnPickupStatus;
import com.readora.delivery.exception.AgentNotFoundException;
import com.readora.delivery.exception.InvalidReturnPickupTransitionException;
import com.readora.delivery.exception.ReturnPickupAlreadyClaimedException;
import com.readora.delivery.exception.ReturnPickupNotFoundException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnPickupServiceTest {

    @Mock private DeliveryAgentRepository agentRepository;
    @Mock private ReturnPickupAssignmentRepository pickupRepository;
    @Mock private CommerceClient commerceClient;

    private ReturnPickupService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ReturnPickupService(agentRepository, pickupRepository, commerceClient, new ObjectMapper());
    }

    private ReturnPickupAssignment newPickup(UUID id) {
        ReturnPickupAssignment pickup = new ReturnPickupAssignment(UUID.randomUUID(), "ORD-1", storeId,
                "Bengaluru", "Ravi Kumar", "999", "[{\"title\":\"Clean Code\",\"qty\":1}]", new BigDecimal("40.00"));
        ReflectionTestUtils.setField(pickup, "id", id);
        return pickup;
    }

    @Test
    void getQueue_agentOffDuty_returnsEmpty() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "999", storeId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));

        assertThat(service.getQueue(userId)).isEmpty();
        verify(pickupRepository, never()).findAllByStoreIdAndStatusOrderByCreatedAt(any(), any());
    }

    @Test
    void getMine_mapsAllPickups() {
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", "999", storeId)));
        when(pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(newPickup(UUID.randomUUID())));

        assertThat(service.getMine(userId)).hasSize(1);
    }

    @Test
    void getDetail_notFound_throws() {
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", "999", storeId)));
        when(pickupRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(userId, UUID.randomUUID())).isInstanceOf(ReturnPickupNotFoundException.class);
    }

    @Test
    void claim_unassignedPickup_succeedsAndNotifiesCommerceService() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "999", storeId);
        UUID pickupId = UUID.randomUUID();
        ReturnPickupAssignment pickup = newPickup(pickupId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(pickup));

        service.claim(userId, pickupId);

        assertThat(pickup.getStatus()).isEqualTo(ReturnPickupStatus.ASSIGNED);
        verify(commerceClient).updateReturnStatus(pickup.getOrderId(), "RETURN_ASSIGNED", userId, "Agent Smith");
    }

    @Test
    void claim_alreadyClaimed_throws() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "999", storeId);
        UUID pickupId = UUID.randomUUID();
        ReturnPickupAssignment pickup = newPickup(pickupId);
        pickup.claim(UUID.randomUUID(), "Someone Else");
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(pickupRepository.findById(pickupId)).thenReturn(Optional.of(pickup));

        assertThatThrownBy(() -> service.claim(userId, pickupId)).isInstanceOf(ReturnPickupAlreadyClaimedException.class);
    }

    @Test
    void markEnRoute_fromWrongState_throws() {
        UUID pickupId = UUID.randomUUID();
        ReturnPickupAssignment pickup = newPickup(pickupId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", "999", storeId)));
        when(pickupRepository.findByIdAndAgentId(pickupId, userId)).thenReturn(Optional.of(pickup));

        assertThatThrownBy(() -> service.markEnRoute(userId, pickupId)).isInstanceOf(InvalidReturnPickupTransitionException.class);
    }

    @Test
    void markEnRoute_fromAssigned_succeeds() {
        UUID pickupId = UUID.randomUUID();
        ReturnPickupAssignment pickup = newPickup(pickupId);
        pickup.claim(userId, "Agent Smith");
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", "999", storeId)));
        when(pickupRepository.findByIdAndAgentId(pickupId, userId)).thenReturn(Optional.of(pickup));

        service.markEnRoute(userId, pickupId);

        assertThat(pickup.getStatus()).isEqualTo(ReturnPickupStatus.EN_ROUTE);
        verify(commerceClient).updateReturnStatus(pickup.getOrderId(), "RETURN_EN_ROUTE", null, null);
    }

    @Test
    void markCollected_fromEnRoute_succeeds() {
        UUID pickupId = UUID.randomUUID();
        ReturnPickupAssignment pickup = newPickup(pickupId);
        pickup.claim(userId, "Agent Smith");
        pickup.markEnRoute();
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", "999", storeId)));
        when(pickupRepository.findByIdAndAgentId(pickupId, userId)).thenReturn(Optional.of(pickup));

        service.markCollected(userId, pickupId);

        assertThat(pickup.getStatus()).isEqualTo(ReturnPickupStatus.COLLECTED);
        verify(commerceClient).updateReturnStatus(pickup.getOrderId(), "RETURN_COLLECTED", null, null);
    }

    @Test
    void markCollected_fromWrongState_throws() {
        UUID pickupId = UUID.randomUUID();
        ReturnPickupAssignment pickup = newPickup(pickupId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", "999", storeId)));
        when(pickupRepository.findByIdAndAgentId(pickupId, userId)).thenReturn(Optional.of(pickup));

        assertThatThrownBy(() -> service.markCollected(userId, pickupId)).isInstanceOf(InvalidReturnPickupTransitionException.class);
    }
}
