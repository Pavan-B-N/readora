package com.readora.delivery.repository;

import com.readora.delivery.entity.ReturnPickupAssignment;
import com.readora.delivery.entity.ReturnPickupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// CRUD and query access to return pickup assignments.
public interface ReturnPickupAssignmentRepository extends JpaRepository<ReturnPickupAssignment, UUID> {

    // Looks up the pickup assignment for a given order, if one exists.
    Optional<ReturnPickupAssignment> findByOrderId(UUID orderId);

    // Returns a store's pickups in a given status, oldest first.
    List<ReturnPickupAssignment> findAllByStoreIdAndStatusOrderByCreatedAt(UUID storeId, ReturnPickupStatus status);

    // Returns an agent's pickups, newest first.
    List<ReturnPickupAssignment> findAllByAgentIdOrderByCreatedAtDesc(UUID agentId);

    // Looks up a pickup scoped to the claiming agent, for ownership checks.
    Optional<ReturnPickupAssignment> findByIdAndAgentId(UUID id, UUID agentId);

    // Counts an agent's pickups in a given status.
    long countByAgentIdAndStatus(UUID agentId, ReturnPickupStatus status);

    /** Sums each row's own snapshotted payout — falls back to 40.00 per row predating that column, matching ReturnPickupService's read-side fallback. */
    @Query("SELECT COALESCE(SUM(COALESCE(p.payoutAmount, 40.00)), 0) FROM ReturnPickupAssignment p WHERE p.agentId = :agentId AND p.status = :status")
    BigDecimal sumPayoutByAgentIdAndStatus(@Param("agentId") UUID agentId, @Param("status") ReturnPickupStatus status);
}
