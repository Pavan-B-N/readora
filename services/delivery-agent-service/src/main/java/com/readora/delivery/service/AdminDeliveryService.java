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
import com.readora.sharedcore.security.CurrentUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Store-scoped, read-only visibility into delivery agents for an admin — who's on duty and what they're carrying — same visibility-not-control separation as the customer-facing admin returns view; claiming/advancing work stays the agent app's job.
@Service
public class AdminDeliveryService {

    private final DeliveryAgentRepository agentRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final ReturnPickupAssignmentRepository pickupRepository;
    private final UserServiceClient userServiceClient;

    // Wires the repositories and user-service client used to resolve admin scope.
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

    // Lists the calling admin's store's agents along with whatever each is currently carrying.
    @Transactional(readOnly = true)
    public List<AdminAgentResponse> listAgents() {
        UUID storeId = resolveCallerStoreId();
        return agentRepository.findAllByStoreId(storeId).stream()
                .map(this::toResponse)
                .toList();
    }

    // Resolves the store the calling admin is scoped to, or throws if they have none.
    private UUID resolveCallerStoreId() {
        UUID storeId = userServiceClient.getAdminStoreId(CurrentUserContext.require());
        if (storeId == null) {
            throw new AdminStoreNotAssignedException();
        }
        return storeId;
    }

    // Builds the admin-facing agent response, including any active delivery or pickup.
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

    // Most recent non-COLLECTED pickup, if any — same one-at-a-time assumption as findActiveDelivery.
    private Optional<ReturnPickupAssignment> findActivePickup(UUID agentId) {
        return pickupRepository.findAllByAgentIdOrderByCreatedAtDesc(agentId).stream()
                .filter(p -> p.getStatus() != ReturnPickupStatus.COLLECTED)
                .findFirst();
    }
}
