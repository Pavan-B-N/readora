package com.readora.delivery.repository;

import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.entity.ReturnPickupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnPickupAssignmentRepository extends JpaRepository<ReturnPickupAssignment, UUID> {

    Optional<ReturnPickupAssignment> findByOrderId(UUID orderId);

    List<ReturnPickupAssignment> findAllByStoreIdAndStatusOrderByCreatedAt(UUID storeId, ReturnPickupStatus status);

    List<ReturnPickupAssignment> findAllByAgentIdOrderByCreatedAtDesc(UUID agentId);

    Optional<ReturnPickupAssignment> findByIdAndAgentId(UUID id, UUID agentId);

    long countByAgentIdAndStatus(UUID agentId, ReturnPickupStatus status);
}
