package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One row per (user, query) — re-searching the same term updates searchedAt in place rather than appending a new row, same convention as BrowsingHistoryItem. */
@Entity
@Table(name = "search_history", schema = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "query"}))
public class SearchHistoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "query", nullable = false)
    private String query;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    // JPA no-arg constructor.
    protected SearchHistoryItem() {
    }

    // Creates a new search history entry, searched now.
    public SearchHistoryItem(UUID userId, String query) {
        this.userId = userId;
        this.query = query;
        this.searchedAt = Instant.now();
    }

    // Bumps the searched timestamp to now.
    public void touch() {
        this.searchedAt = Instant.now();
    }

    // Entry id.
    public UUID getId() {
        return id;
    }

    // Owning user id.
    public UUID getUserId() {
        return userId;
    }

    // Search term.
    public String getQuery() {
        return query;
    }

    // When the term was last searched.
    public Instant getSearchedAt() {
        return searchedAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SearchHistoryItem that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Entity hash by id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
