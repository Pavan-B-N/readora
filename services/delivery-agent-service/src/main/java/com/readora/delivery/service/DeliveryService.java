package com.readora.delivery.service;

import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.dto.AgentMeResponse;
import com.readora.delivery.dto.AssignmentDetailResponse;
import com.readora.delivery.dto.AssignmentResponse;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.exception.AgentNotFoundException;
import com.readora.delivery.exception.AssignmentAlreadyClaimedException;
import com.readora.delivery.exception.AssignmentNotFoundException;
import com.readora.delivery.exception.InvalidAssignmentTransitionException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryService {

    /** Flat, mock payout per delivery — this is a portfolio simulation, not a real payout engine. */
    private static final BigDecimal DELIVERY_PAYOUT = new BigDecimal("40.00");

    private final DeliveryAgentRepository agentRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final CommerceClient commerceClient;

    public DeliveryService(
            DeliveryAgentRepository agentRepository,
            DeliveryAssignmentRepository assignmentRepository,
            CommerceClient commerceClient
    ) {
        this.agentRepository = agentRepository;
        this.assignmentRepository = assignmentRepository;
        this.commerceClient = commerceClient;
    }

    private DeliveryAgent requireAgent(UUID userId) {
        return agentRepository.findById(userId).orElseThrow(AgentNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public AgentMeResponse getMe(UUID userId) {
        DeliveryAgent agent = requireAgent(userId);
        return toMeResponse(agent);
    }

    @Transactional
    public AgentMeResponse setOnDuty(UUID userId, boolean onDuty) {
        DeliveryAgent agent = requireAgent(userId);
        agent.setOnDuty(onDuty);
        agentRepository.save(agent);
        return toMeResponse(agent);
    }

    /**
     * Every UNASSIGNED order at the caller's store — the shared claim queue. Empty while off
     * duty, same as a real gig-delivery app: going online is what makes new work visible.
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getQueue(UUID userId) {
        DeliveryAgent agent = requireAgent(userId);
        if (!agent.isOnDuty()) {
            return List.of();
        }
        return assignmentRepository
                .findAllByStoreIdAndStatusOrderByCreatedAt(agent.getStoreId(), DeliveryAssignmentStatus.UNASSIGNED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Everything the caller has claimed, most recent first, any status. */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMine(UUID userId) {
        requireAgent(userId);
        return assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentDetailResponse getDetail(UUID userId, UUID assignmentId) {
        DeliveryAgent agent = requireAgent(userId);
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getStoreId().equals(agent.getStoreId()))
                .orElseThrow(AssignmentNotFoundException::new);

        return new AssignmentDetailResponse(toResponse(assignment), commerceClient.getDeliveryDetail(assignment.getOrderId()));
    }

    @Transactional
    public AssignmentResponse claim(UUID userId, UUID assignmentId) {
        DeliveryAgent agent = requireAgent(userId);
        DeliveryAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getStoreId().equals(agent.getStoreId()))
                .orElseThrow(AssignmentNotFoundException::new);

        if (assignment.getStatus() != DeliveryAssignmentStatus.UNASSIGNED) {
            throw new AssignmentAlreadyClaimedException();
        }

        assignment.claim(userId);
        assignmentRepository.save(assignment);
        commerceClient.updateDeliveryStatus(assignment.getOrderId(), "ASSIGNED", userId, agent.getName());

        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse markOutForDelivery(UUID userId, UUID assignmentId) {
        requireAgent(userId);
        DeliveryAssignment assignment = assignmentRepository.findByIdAndAgentId(assignmentId, userId)
                .orElseThrow(AssignmentNotFoundException::new);

        if (assignment.getStatus() != DeliveryAssignmentStatus.ASSIGNED) {
            throw new InvalidAssignmentTransitionException();
        }

        assignment.markOutForDelivery();
        assignmentRepository.save(assignment);
        commerceClient.updateDeliveryStatus(assignment.getOrderId(), "SHIPPED", null, null);

        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse markDelivered(UUID userId, UUID assignmentId) {
        requireAgent(userId);
        DeliveryAssignment assignment = assignmentRepository.findByIdAndAgentId(assignmentId, userId)
                .orElseThrow(AssignmentNotFoundException::new);

        if (assignment.getStatus() != DeliveryAssignmentStatus.OUT_FOR_DELIVERY) {
            throw new InvalidAssignmentTransitionException();
        }

        assignment.markDelivered();
        assignmentRepository.save(assignment);
        commerceClient.updateDeliveryStatus(assignment.getOrderId(), "DELIVERED", null, null);

        return toResponse(assignment);
    }

    private AssignmentResponse toResponse(DeliveryAssignment a) {
        return new AssignmentResponse(
                a.getId(), a.getOrderId(), a.getOrderNumber(), a.getStoreId(), a.getStatus().name(),
                a.getCreatedAt(), a.getAssignedAt(), a.getOutForDeliveryAt(), a.getDeliveredAt(),
                a.getDestinationCity(), DELIVERY_PAYOUT
        );
    }

    private AgentMeResponse toMeResponse(DeliveryAgent agent) {
        return new AgentMeResponse(agent.getUserId(), agent.getName(), agent.getPhone(), agent.getStoreId(), agent.isOnDuty());
    }
}
