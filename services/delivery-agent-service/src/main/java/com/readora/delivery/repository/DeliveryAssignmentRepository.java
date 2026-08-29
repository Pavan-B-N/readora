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

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    Optional<DeliveryAssignment> findByOrderId(UUID orderId);

    List<DeliveryAssignment> findAllByStoreIdAndStatusOrderByCreatedAt(UUID storeId, DeliveryAssignmentStatus status);

    List<DeliveryAssignment> findAllByAgentIdOrderByCreatedAtDesc(UUID agentId);

    Optional<DeliveryAssignment> findByIdAndAgentId(UUID id, UUID agentId);

    long countByAgentIdAndStatus(UUID agentId, DeliveryAssignmentStatus status);

    /** Sums each row's own snapshotted payout — falls back to 40.00 per row predating that column, matching DeliveryService's read-side fallback. */
    @Query("SELECT COALESCE(SUM(COALESCE(a.payoutAmount, 40.00)), 0) FROM DeliveryAssignment a WHERE a.agentId = :agentId AND a.status = :status")
    BigDecimal sumPayoutByAgentIdAndStatus(@Param("agentId") UUID agentId, @Param("status") DeliveryAssignmentStatus status);
}
