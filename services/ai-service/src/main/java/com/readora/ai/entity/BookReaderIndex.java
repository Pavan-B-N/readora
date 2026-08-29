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

/**
 * Tracks whether a book's virtual edition content has been embedded for the in-reader RAG
 * assistant — one row per book, shared by every owner (the content is identical for everyone who
 * bought it, so it's only ever embedded once, not per reader).
 */
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

    protected BookReaderIndex() {
    }

    public BookReaderIndex(UUID bookId) {
        this.bookId = bookId;
        this.status = BookReaderIndexStatus.PENDING;
        this.chunkCount = 0;
    }

    public void markReady(int chunkCount) {
        this.status = BookReaderIndexStatus.READY;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = BookReaderIndexStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    public UUID getBookId() {
        return bookId;
    }

    public BookReaderIndexStatus getStatus() {
        return status;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BookReaderIndex that)) return false;
        return bookId != null && Objects.equals(bookId, that.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bookId);
    }
}
