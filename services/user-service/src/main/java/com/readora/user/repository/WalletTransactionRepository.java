package com.readora.user.repository;

import com.readora.user.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Page<WalletTransaction> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
