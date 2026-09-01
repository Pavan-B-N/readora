package com.readora.delivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.dto.AssignmentResponse;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.exception.AssignmentAlreadyClaimedException;
import com.readora.delivery.exception.AssignmentNotFoundException;
import com.readora.delivery.exception.CannotGoOffDutyException;
import com.readora.delivery.exception.InvalidAssignmentTransitionException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
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
class DeliveryServiceTest {

    @Mock
    private DeliveryAgentRepository agentRepository;
    @Mock
    private DeliveryAssignmentRepository assignmentRepository;
    @Mock
    private ReturnPickupAssignmentRepository pickupRepository;
    @Mock
    private CommerceClient commerceClient;

    private DeliveryService deliveryService;

    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(agentRepository, assignmentRepository, pickupRepository, commerceClient, new ObjectMapper());
    }

    private DeliveryAssignment newAssignment(UUID id) {
        DeliveryAssignment assignment = new DeliveryAssignment(UUID.randomUUID(), "ORD-1", storeId, "Bengaluru", "Ravi Kumar", "9999999999", "[{\"title\":\"Clean Code\",\"qty\":1}]", new BigDecimal("40.00"));
        ReflectionTestUtils.setField(assignment, "id", id);
        return assignment;
    }

    @Test
    void getMe_agentNotFound_throws() {
        when(agentRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getMe(userId))
                .isInstanceOf(com.readora.delivery.exception.AgentNotFoundException.class);
    }

    @Test
    void getMe_found_mapsToResponse() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));

        var response = deliveryService.getMe(userId);

        assertThat(response.name()).isEqualTo("Agent Smith");
    }

    @Test
    void getMine_mapsAllAssignmentsRegardlessOfStatus() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(newAssignment(UUID.randomUUID())));

        var mine = deliveryService.getMine(userId);

        assertThat(mine).hasSize(1);
    }

    @Test
    void getDetail_assignmentAtAnotherStore_isTreatedAsNotFound() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignmentAtOtherStore = new DeliveryAssignment(UUID.randomUUID(), "ORD-3", UUID.randomUUID(),
                "Mumbai", "Asha Rao", "8888888888", "[]", new BigDecimal("40.00"));
        ReflectionTestUtils.setField(assignmentAtOtherStore, "id", assignmentId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignmentAtOtherStore));

        assertThatThrownBy(() -> deliveryService.getDetail(userId, assignmentId)).isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void getDetail_ownStoreAssignment_returnsDetailWithCommerceLookup() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignment = newAssignment(assignmentId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(commerceClient.getDeliveryDetail(assignment.getOrderId())).thenReturn(
                org.mockito.Mockito.mock(com.readora.delivery.dto.OrderDeliveryDetailResponse.class));

        var detail = deliveryService.getDetail(userId, assignmentId);

        assertThat(detail.assignment().id()).isEqualTo(assignmentId);
    }

    @Test
    void toResponse_malformedItemsJson_degradesToEmptyListRatherThanFailing() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        DeliveryAssignment assignment = new DeliveryAssignment(UUID.randomUUID(), "ORD-4", storeId,
                "Bengaluru", "Ravi Kumar", "9999999999", "not valid json", new BigDecimal("40.00"));
        UUID assignmentId = UUID.randomUUID();
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(commerceClient.getDeliveryDetail(any())).thenReturn(
                org.mockito.Mockito.mock(com.readora.delivery.dto.OrderDeliveryDetailResponse.class));

        var detail = deliveryService.getDetail(userId, assignmentId);

        assertThat(detail.assignment().items()).isEmpty();
    }

    @Test
    void markOutForDelivery_fromAssignedState_succeeds() {
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignment = newAssignment(assignmentId);
        assignment.claim(userId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", null, storeId)));
        when(assignmentRepository.findByIdAndAgentId(assignmentId, userId)).thenReturn(Optional.of(assignment));

        deliveryService.markOutForDelivery(userId, assignmentId);

        assertThat(assignment.getStatus()).isEqualTo(DeliveryAssignmentStatus.OUT_FOR_DELIVERY);
        verify(commerceClient).updateDeliveryStatus(assignment.getOrderId(), "SHIPPED", null, null);
    }

    @Test
    void setOnDuty_goingOffDutyWithAnActiveDelivery_throwsAndLeavesAgentOnDuty() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        DeliveryAssignment activeAssignment = newAssignment(UUID.randomUUID());
        activeAssignment.claim(userId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(activeAssignment));
        when(pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> deliveryService.setOnDuty(userId, false))
                .isInstanceOf(CannotGoOffDutyException.class);

        assertThat(agent.isOnDuty()).isFalse();
        verify(agentRepository, never()).save(any());
    }

    @Test
    void setOnDuty_goingOffDutyWithAnActiveReturnPickup_throws() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        ReturnPickupAssignment activePickup = new ReturnPickupAssignment(
                UUID.randomUUID(), "ORD-9", storeId, "Chennai", "Ravi Kumar", "9999999999", "[{\"title\":\"Effective Java\",\"qty\":1}]", new BigDecimal("40.00")
        );
        activePickup.claim(userId, "Agent Smith");
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(activePickup));

        assertThatThrownBy(() -> deliveryService.setOnDuty(userId, false))
                .isInstanceOf(CannotGoOffDutyException.class);
    }

    @Test
    void setOnDuty_goingOffDutyWithNoActiveWork_succeeds() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        deliveryService.setOnDuty(userId, false);

        assertThat(agent.isOnDuty()).isFalse();
        verify(agentRepository).save(agent);
    }

    @Test
    void setOnDuty_goingOnDuty_isNeverBlockedRegardlessOfActiveWork() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));

        deliveryService.setOnDuty(userId, true);

        assertThat(agent.isOnDuty()).isTrue();
        verify(assignmentRepository, never()).findAllByAgentIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getQueue_agentOffDuty_returnsEmptyWithoutTouchingAssignments() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));

        List<AssignmentResponse> queue = deliveryService.getQueue(userId);

        assertThat(queue).isEmpty();
        verify(assignmentRepository, never()).findAllByStoreIdAndStatusOrderByCreatedAt(any(), any());
    }

    @Test
    void claim_unassignedAssignment_succeedsAndNotifiesCommerceService() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignment = newAssignment(assignmentId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        deliveryService.claim(userId, assignmentId);

        assertThat(assignment.getStatus()).isEqualTo(DeliveryAssignmentStatus.ASSIGNED);
        assertThat(assignment.getAgentId()).isEqualTo(userId);
        verify(commerceClient).updateDeliveryStatus(assignment.getOrderId(), "ASSIGNED", userId, "Agent Smith");
    }

    @Test
    void claim_alreadyClaimedAssignment_throwsWithoutNotifyingCommerceService() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignment = newAssignment(assignmentId);
        assignment.claim(UUID.randomUUID());
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> deliveryService.claim(userId, assignmentId))
                .isInstanceOf(AssignmentAlreadyClaimedException.class);

        verify(commerceClient, never()).updateDeliveryStatus(any(), any(), any(), any());
    }

    @Test
    void claim_assignmentAtAnotherStore_isTreatedAsNotFound() {
        DeliveryAgent agent = new DeliveryAgent(userId, "Agent Smith", "9999999999", storeId);
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignmentAtOtherStore = new DeliveryAssignment(UUID.randomUUID(), "ORD-2", UUID.randomUUID(), "Mumbai", "Asha Rao", "8888888888", "[{\"title\":\"The Pragmatic Programmer\",\"qty\":1}]", new BigDecimal("40.00"));
        ReflectionTestUtils.setField(assignmentAtOtherStore, "id", assignmentId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(agent));
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignmentAtOtherStore));

        assertThatThrownBy(() -> deliveryService.claim(userId, assignmentId))
                .isInstanceOf(AssignmentNotFoundException.class);
    }

    @Test
    void markOutForDelivery_fromUnassignedState_isRejected() {
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignment = newAssignment(assignmentId);
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", null, storeId)));
        when(assignmentRepository.findByIdAndAgentId(assignmentId, userId)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> deliveryService.markOutForDelivery(userId, assignmentId))
                .isInstanceOf(InvalidAssignmentTransitionException.class);
    }

    @Test
    void markDelivered_fromOutForDeliveryState_succeeds() {
        UUID assignmentId = UUID.randomUUID();
        DeliveryAssignment assignment = newAssignment(assignmentId);
        assignment.claim(userId);
        assignment.markOutForDelivery();
        when(agentRepository.findById(userId)).thenReturn(Optional.of(new DeliveryAgent(userId, "Agent Smith", null, storeId)));
        when(assignmentRepository.findByIdAndAgentId(assignmentId, userId)).thenReturn(Optional.of(assignment));

        deliveryService.markDelivered(userId, assignmentId);

        assertThat(assignment.getStatus()).isEqualTo(DeliveryAssignmentStatus.DELIVERED);
        verify(commerceClient).updateDeliveryStatus(assignment.getOrderId(), "DELIVERED", null, null);
    }
}
