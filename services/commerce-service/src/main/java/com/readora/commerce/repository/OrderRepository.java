package com.readora.commerce.repository;

import com.readora.commerce.entity.Order;
import com.readora.commerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Page<Order> findAllByUserIdOrderByPlacedAtDesc(UUID userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    /** Store-scoped, for the admin returns view — mirrors AdminBookService's findByIdAndStoreId ownership pattern. */
    Page<Order> findAllByStoreIdAndStatusInOrderByPlacedAtDesc(UUID storeId, List<OrderStatus> statuses, Pageable pageable);

    /** Pending = cancellations/returns that have not yet been reviewed by an admin. */
    Page<Order> findAllByStoreIdAndStatusInAndAdminReviewedAtIsNullOrderByPlacedAtDesc(UUID storeId, List<OrderStatus> statuses, Pageable pageable);

    /** Reviewed = cancellations/returns where an admin has already recorded a decision/note. */
    Page<Order> findAllByStoreIdAndStatusInAndAdminReviewedAtIsNotNullOrderByPlacedAtDesc(UUID storeId, List<OrderStatus> statuses, Pageable pageable);

    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);
}

