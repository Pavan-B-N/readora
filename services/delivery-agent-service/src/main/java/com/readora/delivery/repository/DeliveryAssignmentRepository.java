package com.readora.delivery.repository;

import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    Optional<DeliveryAssignment> findByOrderId(UUID orderId);

    List<DeliveryAssignment> findAllByStoreIdAndStatusOrderByCreatedAt(UUID storeId, DeliveryAssignmentStatus status);

    List<DeliveryAssignment> findAllByAgentIdOrderByCreatedAtDesc(UUID agentId);

    Optional<DeliveryAssignment> findByIdAndAgentId(UUID id, UUID agentId);
}
