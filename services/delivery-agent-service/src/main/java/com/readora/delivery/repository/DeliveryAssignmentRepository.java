package com.readora.delivery.repository;

import com.readora.delivery.entity.DeliveryAssignment;
import com.readora.delivery.entity.DeliveryAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// CRUD and query access to delivery assignments.
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    // Looks up the assignment for a given order, if one exists.
    Optional<DeliveryAssignment> findByOrderId(UUID orderId);

    // Returns a store's assignments in a given status, oldest first.
    List<DeliveryAssignment> findAllByStoreIdAndStatusOrderByCreatedAt(UUID storeId, DeliveryAssignmentStatus status);

    // Returns an agent's assignments, newest first.
    List<DeliveryAssignment> findAllByAgentIdOrderByCreatedAtDesc(UUID agentId);

    // Looks up an assignment scoped to the claiming agent, for ownership checks.
    Optional<DeliveryAssignment> findByIdAndAgentId(UUID id, UUID agentId);

    // Counts an agent's assignments in a given status.
    long countByAgentIdAndStatus(UUID agentId, DeliveryAssignmentStatus status);

    /** Sums each row's own snapshotted payout — falls back to 40.00 per row predating that column, matching DeliveryService's read-side fallback. */
    @Query("SELECT COALESCE(SUM(COALESCE(a.payoutAmount, 40.00)), 0) FROM DeliveryAssignment a WHERE a.agentId = :agentId AND a.status = :status")
    BigDecimal sumPayoutByAgentIdAndStatus(@Param("agentId") UUID agentId, @Param("status") DeliveryAssignmentStatus status);
}
