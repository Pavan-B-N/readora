package com.readora.commerce.repository;

import com.readora.commerce.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrderId(UUID orderId);

    /** Batch variant for listing a page of orders at once — avoids one query per order. */
    List<OrderItem> findAllByOrderIdIn(List<UUID> orderIds);

    /**
     * Excludes CANCELLED and every return-family status from RETURN_APPROVED onward — but not
     * RETURN_REQUESTED or RETURN_REJECTED, so a customer keeps ebook access while a return is
     * pending admin review, and permanently if it's rejected. See OrderStatus's javadoc.
     */
    @Query("""
            SELECT DISTINCT oi.bookId FROM OrderItem oi
            JOIN oi.order o
            WHERE o.userId = :userId AND o.status NOT IN (
                com.readora.commerce.entity.OrderStatus.CANCELLED,
                com.readora.commerce.entity.OrderStatus.RETURN_APPROVED,
                com.readora.commerce.entity.OrderStatus.RETURN_ASSIGNED,
                com.readora.commerce.entity.OrderStatus.RETURN_EN_ROUTE,
                com.readora.commerce.entity.OrderStatus.RETURN_COLLECTED,
                com.readora.commerce.entity.OrderStatus.REFUND_INITIATED,
                com.readora.commerce.entity.OrderStatus.RETURNED
            )
            """)
    List<UUID> findDistinctBookIdsByUserId(@Param("userId") UUID userId);

    /** Newest-first, every status included (unlike findDistinctBookIdsByUserId) — this backs an order-history rail, so a cancelled/returned item's status is exactly the point of showing it. */
    @Query("""
            SELECT oi FROM OrderItem oi
            JOIN oi.order o
            WHERE o.userId = :userId
            ORDER BY o.placedAt DESC
            """)
    List<OrderItem> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);
}
