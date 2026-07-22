package com.readora.user.repository;

import com.readora.user.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Spring Data repository for wallet transactions.
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    // Paginated, newest-first ledger for a user.
    Page<WalletTransaction> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Whether a transaction with this idempotency key already exists.
    boolean existsByIdempotencyKey(String idempotencyKey);
}
