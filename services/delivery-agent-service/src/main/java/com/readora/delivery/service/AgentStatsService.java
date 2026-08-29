package com.readora.delivery.service;

import com.readora.delivery.dto.AgentStatsResponse;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.entity.ReturnPickupStatus;
import com.readora.delivery.exception.AgentNotFoundException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cross-cutting agent-level aggregation — deliberately its own service rather than living on
 * DeliveryService or ReturnPickupService, since it combines both domains (same reasoning as
 * AdminDeliveryService, which already reads from both repositories for the admin agent list).
 */
@Service
public class AgentStatsService {

    private final DeliveryAgentRepository agentRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final ReturnPickupAssignmentRepository pickupRepository;

    public AgentStatsService(
            DeliveryAgentRepository agentRepository,
            DeliveryAssignmentRepository assignmentRepository,
            ReturnPickupAssignmentRepository pickupRepository
    ) {
        this.agentRepository = agentRepository;
        this.assignmentRepository = assignmentRepository;
        this.pickupRepository = pickupRepository;
    }

    @Transactional(readOnly = true)
    public AgentStatsResponse getStats(UUID userId) {
        agentRepository.findById(userId).orElseThrow(AgentNotFoundException::new);

        long completedDeliveries = assignmentRepository.countByAgentIdAndStatus(userId, DeliveryAssignmentStatus.DELIVERED);
        long completedReturnPickups = pickupRepository.countByAgentIdAndStatus(userId, ReturnPickupStatus.COLLECTED);

        BigDecimal totalEarnings = DeliveryService.DELIVERY_PAYOUT.multiply(BigDecimal.valueOf(completedDeliveries))
                .add(ReturnPickupService.PICKUP_PAYOUT.multiply(BigDecimal.valueOf(completedReturnPickups)));

        return new AgentStatsResponse((int) completedDeliveries, (int) completedReturnPickups, totalEarnings, "INR");
    }
}
