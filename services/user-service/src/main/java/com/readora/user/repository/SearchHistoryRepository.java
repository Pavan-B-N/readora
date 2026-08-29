package com.readora.user.repository;

import com.readora.user.entity.SearchHistoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SearchHistoryRepository extends JpaRepository<SearchHistoryItem, UUID> {

    /** Capped at 20 — a "recent searches" list, not a full search log. */
    List<SearchHistoryItem> findTop20ByUserIdOrderBySearchedAtDesc(UUID userId);

    Optional<SearchHistoryItem> findByUserIdAndQueryIgnoreCase(UUID userId, String query);
}
