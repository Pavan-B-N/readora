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

// A book a user has wishlisted; one row per (user, book).
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

    // JPA no-arg constructor.
    protected WishlistItem() {
    }

    // Creates a new wishlist entry, added now.
    public WishlistItem(UUID userId, UUID bookId) {
        this.userId = userId;
        this.bookId = bookId;
        this.addedAt = Instant.now();
    }

    // Entry id.
    public UUID getId() {
        return id;
    }

    // Owning user id.
    public UUID getUserId() {
        return userId;
    }

    // Wishlisted book id.
    public UUID getBookId() {
        return bookId;
    }

    // When the book was added.
    public Instant getAddedAt() {
        return addedAt;
    }

    // Entity equality by id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WishlistItem that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Entity hash by id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
