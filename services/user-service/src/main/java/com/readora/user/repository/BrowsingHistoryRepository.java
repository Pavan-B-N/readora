package com.readora.user.repository;

import com.readora.user.entity.BrowsingHistoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Spring Data repository for browsing history.
public interface BrowsingHistoryRepository extends JpaRepository<BrowsingHistoryItem, UUID> {

    // Capped at 20 — a "recently viewed" rail, not a full history log.
    List<BrowsingHistoryItem> findTop20ByUserIdOrderByViewedAtDesc(UUID userId);

    // Looks up an existing history entry for a (user, book) pair, for the upsert-on-view path.
    Optional<BrowsingHistoryItem> findByUserIdAndBookId(UUID userId, UUID bookId);
}
