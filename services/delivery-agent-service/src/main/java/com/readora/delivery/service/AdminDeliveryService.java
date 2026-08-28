package com.readora.delivery.service;

import com.readora.delivery.client.UserServiceClient;
import com.readora.delivery.dto.AdminAgentResponse;
import com.readora.delivery.entity.DeliveryAgent;
import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.entity.ReturnPickupStatus;
import com.readora.delivery.exception.AdminStoreNotAssignedException;
import com.readora.delivery.repository.DeliveryAgentRepository;
import com.readora.delivery.repository.DeliveryAssignmentRepository;
import com.readora.delivery.repository.ReturnPickupAssignmentRepository;
import com.readora.delivery.security.CurrentUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Store-scoped visibility into delivery agents for an admin — who's on duty and what they're
 * carrying right now. Read-only: an admin can look but the agent's own app is still the only way
 * to claim/advance work, same separation of concerns as the customer-facing admin returns view
 * (visibility, not control).
 */
@Service
public class AdminDeliveryService {

    private final DeliveryAgentRepository agentRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final ReturnPickupAssignmentRepository pickupRepository;
    private final UserServiceClient userServiceClient;

    public AdminDeliveryService(
            DeliveryAgentRepository agentRepository,
            DeliveryAssignmentRepository assignmentRepository,
            ReturnPickupAssignmentRepository pickupRepository,
            UserServiceClient userServiceClient
    ) {
        this.agentRepository = agentRepository;
        this.assignmentRepository = assignmentRepository;
        this.pickupRepository = pickupRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional(readOnly = true)
    public List<AdminAgentResponse> listAgents() {
        UUID storeId = resolveCallerStoreId();
        return agentRepository.findAllByStoreId(storeId).stream()
                .map(this::toResponse)
                .toList();
    }

    private UUID resolveCallerStoreId() {
        UUID storeId = userServiceClient.getAdminStoreId(CurrentUserContext.require());
        if (storeId == null) {
            throw new AdminStoreNotAssignedException();
        }
        return storeId;
    }

    private AdminAgentResponse toResponse(DeliveryAgent agent) {
        AdminAgentResponse.ActiveWork activeWork = findActiveDelivery(agent.getUserId())
                .map(a -> new AdminAgentResponse.ActiveWork("DELIVERY", a.getOrderNumber(), a.getStatus().name(), a.getDestinationCity()))
                .or(() -> findActivePickup(agent.getUserId())
                        .map(p -> new AdminAgentResponse.ActiveWork("RETURN_PICKUP", p.getOrderNumber(), p.getStatus().name(), p.getDestinationCity())))
                .orElse(null);

        return new AdminAgentResponse(agent.getUserId(), agent.getName(), agent.getPhone(), agent.isOnDuty(), activeWork);
    }

    /** Most recent non-DELIVERED assignment, if any — an agent only ever carries one order at a time in this build. */
    private Optional<DeliveryAssignment> findActiveDelivery(UUID agentId) {
        return assignmentRepository.findAllByAgentIdOrderByCreatedAtDesc(agentId).stream()
                .filter(a -> a.getStatus() != DeliveryAssignmentStatus.DELIVERED)
                .findFirst();
    }

    private Optional<ReturnPickupAssignment> findActivePickup(UUID agentId) {
        return pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(agentId).stream()
                .filter(p -> p.getStatus() != ReturnPickupStatus.COLLECTED)
                .findFirst();
    }
}
