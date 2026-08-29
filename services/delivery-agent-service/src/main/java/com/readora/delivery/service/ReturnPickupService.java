package com.readora.delivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.client.CommerceClient;
import com.readora.delivery.dto.ItemSnapshot;
import com.readora.delivery.dto.ReturnPickupDetailResponse;
import com.readora.delivery.dto.ReturnPickupResponse;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.entity.ReturnPickupStatus;
import com.readora.delivery.exception.AgentNotFoundException;
import com.readora.delivery.exception.InvalidReturnPickupTransitionException;
import com.readora.delivery.exception.ReturnPickupAlreadyClaimedException;
import com.readora.delivery.exception.ReturnPickupNotFoundException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Mirrors DeliveryService's shape exactly, for the reverse (pickup) leg of a physical return. */
@Service
public class ReturnPickupService {

    private static final Logger log = LoggerFactory.getLogger(ReturnPickupService.class);

    /** Fallback only, for rows created before payout became a per-assignment snapshot (see V4__payout_amount.sql). */
    public static final BigDecimal PICKUP_PAYOUT = new BigDecimal("40.00");

    private final DeliveryAgentRepository agentRepository;
    private final ReturnPickupAssignmentRepository pickupRepository;
    private final CommerceClient commerceClient;
    private final ObjectMapper objectMapper;

    public ReturnPickupService(
            DeliveryAgentRepository agentRepository,
            ReturnPickupAssignmentRepository pickupRepository,
            CommerceClient commerceClient,
            ObjectMapper objectMapper
    ) {
        this.agentRepository = agentRepository;
        this.pickupRepository = pickupRepository;
        this.commerceClient = commerceClient;
        this.objectMapper = objectMapper;
    }

    private DeliveryAgent requireAgent(UUID userId) {
        return agentRepository.findById(userId).orElseThrow(AgentNotFoundException::new);
    }

    /** Every UNASSIGNED pickup at the caller's store — the shared claim queue. Empty while off duty. */
    @Transactional(readOnly = true)
    public List<ReturnPickupResponse> getQueue(UUID userId) {
        DeliveryAgent agent = requireAgent(userId);
        if (!agent.isOnDuty()) {
            return List.of();
        }
        return pickupRepository
                .findAllByStoreIdAndStatusOrderByCreatedAt(agent.getStoreId(), ReturnPickupStatus.UNASSIGNED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Everything the caller has claimed, most recent first, any status. */
    @Transactional(readOnly = true)
    public List<ReturnPickupResponse> getMine(UUID userId) {
        requireAgent(userId);
        return pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReturnPickupDetailResponse getDetail(UUID userId, UUID pickupId) {
        DeliveryAgent agent = requireAgent(userId);
        ReturnPickupAssignment pickup = pickupRepository.findById(pickupId)
                .filter(p -> p.getStoreId().equals(agent.getStoreId()))
                .orElseThrow(ReturnPickupNotFoundException::new);

        return new ReturnPickupDetailResponse(toResponse(pickup), commerceClient.getDeliveryDetail(pickup.getOrderId()));
    }

    @Transactional
    public ReturnPickupResponse claim(UUID userId, UUID pickupId) {
        DeliveryAgent agent = requireAgent(userId);
        ReturnPickupAssignment pickup = pickupRepository.findById(pickupId)
                .filter(p -> p.getStoreId().equals(agent.getStoreId()))
                .orElseThrow(ReturnPickupNotFoundException::new);

        if (pickup.getStatus() != ReturnPickupStatus.UNASSIGNED) {
            throw new ReturnPickupAlreadyClaimedException();
        }

        pickup.claim(userId, agent.getName());
        pickupRepository.save(pickup);
        commerceClient.updateReturnStatus(pickup.getOrderId(), "RETURN_ASSIGNED", userId, agent.getName());

        return toResponse(pickup);
    }

    @Transactional
    public ReturnPickupResponse markEnRoute(UUID userId, UUID pickupId) {
        requireAgent(userId);
        ReturnPickupAssignment pickup = pickupRepository.findByIdAndAgentId(pickupId, userId)
                .orElseThrow(ReturnPickupNotFoundException::new);

        if (pickup.getStatus() != ReturnPickupStatus.ASSIGNED) {
            throw new InvalidReturnPickupTransitionException();
        }

        pickup.markEnRoute();
        pickupRepository.save(pickup);
        commerceClient.updateReturnStatus(pickup.getOrderId(), "RETURN_EN_ROUTE", null, null);

        return toResponse(pickup);
    }

    @Transactional
    public ReturnPickupResponse markCollected(UUID userId, UUID pickupId) {
        requireAgent(userId);
        ReturnPickupAssignment pickup = pickupRepository.findByIdAndAgentId(pickupId, userId)
                .orElseThrow(ReturnPickupNotFoundException::new);

        if (pickup.getStatus() != ReturnPickupStatus.EN_ROUTE) {
            throw new InvalidReturnPickupTransitionException();
        }

        pickup.markCollected();
        pickupRepository.save(pickup);
        commerceClient.updateReturnStatus(pickup.getOrderId(), "RETURN_COLLECTED", null, null);

        return toResponse(pickup);
    }

    private ReturnPickupResponse toResponse(ReturnPickupAssignment p) {
        return new ReturnPickupResponse(
                p.getId(), p.getOrderId(), p.getOrderNumber(), p.getStoreId(), p.getStatus().name(),
                p.getCreatedAt(), p.getAssignedAt(), p.getEnRouteAt(), p.getCollectedAt(),
                p.getDestinationCity(), p.getRecipientName(), p.getRecipientPhone(), parseItems(p.getItemsJson()),
                p.getPayoutAmount() != null ? p.getPayoutAmount() : PICKUP_PAYOUT
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
}
