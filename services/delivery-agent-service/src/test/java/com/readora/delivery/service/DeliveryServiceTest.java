package com.readora.delivery.service;

import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.dto.AssignmentResponse;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.exception.AssignmentAlreadyClaimedException;
import com.readora.delivery.exception.AssignmentNotFoundException;
import com.readora.delivery.exception.InvalidAssignmentTransitionException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private CommerceClient commerceClient;

    private DeliveryService deliveryService;

    private final UUID userId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        deliveryService = new DeliveryService(agentRepository, assignmentRepository, commerceClient);
    }

    private DeliveryAssignment newAssignment(UUID id) {
        DeliveryAssignment assignment = new DeliveryAssignment(UUID.randomUUID(), "ORD-1", storeId, "Bengaluru");
        ReflectionTestUtils.setField(assignment, "id", id);
        return assignment;
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
        DeliveryAssignment assignmentAtOtherStore = new DeliveryAssignment(UUID.randomUUID(), "ORD-2", UUID.randomUUID(), "Mumbai");
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
