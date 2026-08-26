package com.readora.commerce.repository;

import com.readora.commerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrderId(UUID orderId);

    @Query("""
            SELECT DISTINCT oi.bookId FROM OrderItem oi
            JOIN oi.order o
            WHERE o.userId = :userId AND o.status NOT IN (com.readora.commerce.entity.OrderStatus.CANCELLED, com.readora.commerce.entity.OrderStatus.RETURNED)
            """)
    List<UUID> findDistinctBookIdsByUserId(@Param("userId") UUID userId);
}
