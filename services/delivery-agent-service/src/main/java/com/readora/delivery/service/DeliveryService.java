package com.readora.delivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.dto.AgentMeResponse;
import com.readora.delivery.dto.AssignmentDetailResponse;
import com.readora.delivery.dto.AssignmentResponse;
import com.readora.delivery.dto.ItemSnapshot;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.entity.ReturnPickupStatus;
import com.readora.delivery.exception.AgentNotFoundException;
import com.readora.delivery.exception.AssignmentAlreadyClaimedException;
import com.readora.delivery.exception.AssignmentNotFoundException;
import com.readora.delivery.exception.CannotGoOffDutyException;
import com.readora.delivery.exception.InvalidAssignmentTransitionException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    /** Fallback only, for rows created before payout became a per-assignment snapshot (see V4__payout_amount.sql). */
    public static final BigDecimal DELIVERY_PAYOUT = new BigDecimal("40.00");

    private final DeliveryAgentRepository agentRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final ReturnPickupAssignmentRepository pickupRepository;
    private final CommerceClient commerceClient;
    private final ObjectMapper objectMapper;

    public DeliveryService(
            DeliveryAgentRepository agentRepository,
            DeliveryAssignmentRepository assignmentRepository,
            ReturnPickupAssignmentRepository pickupRepository,
            CommerceClient commerceClient,
            ObjectMapper objectMapper
    ) {
        this.agentRepository = agentRepository;
        this.assignmentRepository = assignmentRepository;
        this.pickupRepository = pickupRepository;
        this.commerceClient = commerceClient;
        this.objectMapper = objectMapper;
    }

    private DeliveryAgent requireAgent(UUID userId) {
        return agentRepository.findById(userId).orElseThrow(AgentNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public AgentMeResponse getMe(UUID userId) {
        DeliveryAgent agent = requireAgent(userId);
        return toMeResponse(agent);
    }

    /**
     * Going online is always allowed. Going offline is blocked while the agent has a
     * claimed-but-not-finished delivery or pickup — otherwise they could clock out mid-job and
     * leave a customer's order stranded with nobody responsible for finishing it.
     */
    @Transactional
    public AgentMeResponse setOnDuty(UUID userId, boolean onDuty) {
        DeliveryAgent agent = requireAgent(userId);
        if (!onDuty && hasActiveWork(userId)) {
            throw new CannotGoOffDutyException();
        }
        agent.setOnDuty(onDuty);
        agentRepository.save(agent);
        return toMeResponse(agent);
    }

    private boolean hasActiveWork(UUID agentId) {
        boolean hasActiveDelivery = assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(agentId).stream()
                .anyMatch(a -> a.getStatus() == DeliveryAssignmentStatus.ASSIGNED || a.getStatus() == DeliveryAssignmentStatus.OUT_FOR_DELIVERY);
        boolean hasActivePickup = pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(agentId).stream()
                .anyMatch(p -> p.getStatus() == ReturnPickupStatus.ASSIGNED || p.getStatus() == ReturnPickupStatus.EN_ROUTE);
        return hasActiveDelivery || hasActivePickup;
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
                a.getDestinationCity(), a.getRecipientName(), a.getRecipientPhone(), parseItems(a.getItemsJson()),
                a.getPayoutAmount() != null ? a.getPayoutAmount() : DELIVERY_PAYOUT
        );
    }

    /** Defensive: a parse failure (missing data, or a row written before this format existed) degrades to an empty list, never a 500. */
    private List<ItemSnapshot> parseItems(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(itemsJson, ItemSnapshot[].class));
        } catch (Exception e) {
            log.warn("Could not parse stored items JSON: {}", itemsJson, e);
            return List.of();
        }
    }

    private AgentMeResponse toMeResponse(DeliveryAgent agent) {
        return new AgentMeResponse(agent.getUserId(), agent.getName(), agent.getPhone(), agent.getStoreId(), agent.isOnDuty());
    }
}
