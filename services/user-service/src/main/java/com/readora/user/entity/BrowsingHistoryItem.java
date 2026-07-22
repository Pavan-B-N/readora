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

/** One row per (user, book) — re-viewing a book updates viewedAt in place rather than appending a new row. */
@Entity
@Table(name = "browsing_history", schema = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
public class BrowsingHistoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Cross-service reference to catalog.books, unconstrained — same reasoning as elsewhere in this build. */
    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    // JPA no-arg constructor.
    protected BrowsingHistoryItem() {
    }

    // Creates a new browsing history entry, viewed now.
    public BrowsingHistoryItem(UUID userId, UUID bookId) {
        this.userId = userId;
        this.bookId = bookId;
        this.viewedAt = Instant.now();
    }

    // Bumps the viewed timestamp to now.
    public void touch() {
        this.viewedAt = Instant.now();
    }

    // Entry id.
    public UUID getId() {
        return id;
    }

    // Owning user id.
    public UUID getUserId() {
        return userId;
    }

    // Viewed book id.
    public UUID getBookId() {
        return bookId;
    }

    // When the book was last viewed.
    public Instant getViewedAt() {
        return viewedAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BrowsingHistoryItem that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Entity hash by id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
