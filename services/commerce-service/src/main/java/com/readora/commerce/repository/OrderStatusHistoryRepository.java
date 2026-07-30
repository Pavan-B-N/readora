package com.readora.commerce.repository;

import com.readora.commerce.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Data access for an order's status-transition audit trail.
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    // All transitions for one order, oldest first.
    List<OrderStatusHistory> findAllByOrderIdOrderByChangedAt(UUID orderId);
}
