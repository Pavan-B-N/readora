package com.readora.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Tracks whether a book's virtual edition content has been embedded for the in-reader RAG assistant — one row per book, shared by every owner since the content is identical for everyone who bought it, so it's only ever embedded once, not per reader.
@Entity
@Table(name = "book_reader_index", schema = "ai")
public class BookReaderIndex {

    @Id
    @Column(name = "book_id")
    private UUID bookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookReaderIndexStatus status;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA no-arg constructor.
    protected BookReaderIndex() {
    }

    // Creates a new index row in PENDING status for a book.
    public BookReaderIndex(UUID bookId) {
        this.bookId = bookId;
        this.status = BookReaderIndexStatus.PENDING;
        this.chunkCount = 0;
    }

    // Marks the index READY with the given chunk count and clears any prior error.
    public void markReady(int chunkCount) {
        this.status = BookReaderIndexStatus.READY;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }

    // Marks the index FAILED with the given error message.
    public void markFailed(String errorMessage) {
        this.status = BookReaderIndexStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    // Stamps createdAt (first save only) and updatedAt on every insert/update.
    @PrePersist
    @PreUpdate
    protected void onSave() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    // Gets the book id.
    public UUID getBookId() {
        return bookId;
    }

    // Gets the indexing status.
    public BookReaderIndexStatus getStatus() {
        return status;
    }

    // Gets the number of content chunks embedded.
    public int getChunkCount() {
        return chunkCount;
    }

    // Gets the last indexing error message, if any.
    public String getErrorMessage() {
        return errorMessage;
    }

    // Gets when the row was last updated.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Entities are equal when their book id matches.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BookReaderIndex that)) return false;
        return bookId != null && Objects.equals(bookId, that.bookId);
    }

    // Hashes on the book id.
    @Override
    public int hashCode() {
        return Objects.hashCode(bookId);
    }
}
