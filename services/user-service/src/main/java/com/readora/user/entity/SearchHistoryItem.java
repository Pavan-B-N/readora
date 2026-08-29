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

    protected SearchHistoryItem() {
    }

    public SearchHistoryItem(UUID userId, String query) {
        this.userId = userId;
        this.query = query;
        this.searchedAt = Instant.now();
    }

    public void touch() {
        this.searchedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getQuery() {
        return query;
    }

    public Instant getSearchedAt() {
        return searchedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SearchHistoryItem that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
