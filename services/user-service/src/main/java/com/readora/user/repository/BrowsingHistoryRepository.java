package com.readora.user.repository;

import com.readora.user.entity.BrowsingHistoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrowsingHistoryRepository extends JpaRepository<BrowsingHistoryItem, UUID> {

    /** Capped at 20 — a "recently viewed" rail, not a full history log. */
    List<BrowsingHistoryItem> findTop20ByUserIdOrderByViewedAtDesc(UUID userId);

    Optional<BrowsingHistoryItem> findByUserIdAndBookId(UUID userId, UUID bookId);
}
