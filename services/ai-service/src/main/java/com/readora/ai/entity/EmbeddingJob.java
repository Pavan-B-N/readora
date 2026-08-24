package com.readora.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One backfill run — the audit trail for "who re-embedded the catalogue and when". Progress
 * counters are updated as the consumer works through pages, so the admin UI can poll for a
 * live percentage instead of staring at a spinner.
 */
@Entity
@Table(name = "embedding_jobs", schema = "ai")
public class EmbeddingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmbeddingJobStatus status = EmbeddingJobStatus.QUEUED;

    @Column(name = "triggered_by", nullable = false)
    private UUID triggeredBy;

    @Column(name = "total_books", nullable = false)
    private int totalBooks = 0;

    @Column(name = "processed_books", nullable = false)
    private int processedBooks = 0;

    @Column(name = "current_book_title")
    private String currentBookTitle;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "queued_at", nullable = false, updatable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected EmbeddingJob() {
    }

    public EmbeddingJob(UUID triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    @PrePersist
    protected void onCreate() {
        this.queuedAt = Instant.now();
    }

    public void markRunning() {
        this.status = EmbeddingJobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void recordProgress(int processed, int total, String currentTitle) {
        this.processedBooks = processed;
        this.totalBooks = total;
        this.currentBookTitle = currentTitle;
    }

    public void markCompleted(int total) {
        this.status = EmbeddingJobStatus.COMPLETED;
        this.totalBooks = total;
        this.processedBooks = total;
        this.currentBookTitle = null;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = EmbeddingJobStatus.FAILED;
        this.errorMessage = message;
        this.currentBookTitle = null;
        this.finishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public EmbeddingJobStatus getStatus() {
        return status;
    }

    public UUID getTriggeredBy() {
        return triggeredBy;
    }

    public int getTotalBooks() {
        return totalBooks;
    }

    public int getProcessedBooks() {
        return processedBooks;
    }

    public String getCurrentBookTitle() {
        return currentBookTitle;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EmbeddingJob that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
