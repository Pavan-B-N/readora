package com.readora.user.repository;

import com.readora.user.entity.SearchHistoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Spring Data repository for search history.
public interface SearchHistoryRepository extends JpaRepository<SearchHistoryItem, UUID> {

    // Capped at 20 — a "recent searches" list, not a full search log.
    List<SearchHistoryItem> findTop20ByUserIdOrderBySearchedAtDesc(UUID userId);

    // Case-insensitive lookup of an existing entry for a (user, query) pair, for the upsert-on-search path.
    Optional<SearchHistoryItem> findByUserIdAndQueryIgnoreCase(UUID userId, String query);
}
