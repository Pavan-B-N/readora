package com.readora.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One book embedded during a backfill run — lets the admin UI show a live, book-by-book feed instead of just an aggregate counter. */
@Entity
@Table(name = "embedding_job_book_logs", schema = "ai")
public class EmbeddingJobBookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private EmbeddingJob job;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected EmbeddingJobBookLog() {
    }

    public EmbeddingJobBookLog(EmbeddingJob job, UUID bookId, String title) {
        this.job = job;
        this.bookId = bookId;
        this.title = title;
    }

    @PrePersist
    protected void onCreate() {
        this.processedAt = Instant.now();
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EmbeddingJobBookLog that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
