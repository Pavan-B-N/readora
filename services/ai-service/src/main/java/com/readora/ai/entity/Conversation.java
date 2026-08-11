package com.readora.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// A saved assistant conversation, either general shopping chat or a book reader's Q&A thread.
@Entity
@Table(name = "conversations", schema = "ai")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title")
    private String title;

    /** Null for a general shopping-assistant conversation; set for a book reader's Q&A thread, one per (user, book). */
    @Column(name = "book_id")
    private UUID bookId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA no-arg constructor.
    protected Conversation() {
    }

    // Creates a general shopping-assistant conversation.
    public Conversation(UUID userId, String title) {
        this.userId = userId;
        this.title = title;
    }

    // Creates a book reader Q&A conversation scoped to one book.
    public Conversation(UUID userId, String title, UUID bookId) {
        this.userId = userId;
        this.title = title;
        this.bookId = bookId;
    }

    // Stamps createdAt and updatedAt on insert.
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Refreshes updatedAt on every update.
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Gets the conversation id.
    public UUID getId() {
        return id;
    }

    // Gets the owning user's id.
    public UUID getUserId() {
        return userId;
    }

    // Gets the conversation title.
    public String getTitle() {
        return title;
    }

    // Gets the scoped book id, or null for a general conversation.
    public UUID getBookId() {
        return bookId;
    }

    // Sets the conversation title.
    public void setTitle(String title) {
        this.title = title;
    }

    // Gets when the conversation was last updated.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Entities are equal when their id matches.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Conversation that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hashes on the id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
