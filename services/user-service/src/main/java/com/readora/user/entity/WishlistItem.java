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

@Entity
@Table(name = "wishlist_items", schema = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id"}))
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Cross-service reference to catalog.books, unconstrained — same reasoning as elsewhere in this build. */
    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected WishlistItem() {
    }

    public WishlistItem(UUID userId, UUID bookId) {
        this.userId = userId;
        this.bookId = bookId;
        this.addedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WishlistItem that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
