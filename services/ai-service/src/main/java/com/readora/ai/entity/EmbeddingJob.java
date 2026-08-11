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

// One backfill run — the audit trail for "who re-embedded the catalogue and when"; progress counters are updated as the consumer works through pages, so the admin UI can poll for a live percentage instead of staring at a spinner.
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

    // JPA no-arg constructor.
    protected EmbeddingJob() {
    }

    // Creates a new job in QUEUED status for the triggering admin user.
    public EmbeddingJob(UUID triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    // Stamps queuedAt on insert.
    @PrePersist
    protected void onCreate() {
        this.queuedAt = Instant.now();
    }

    // Transitions the job to RUNNING and stamps startedAt.
    public void markRunning() {
        this.status = EmbeddingJobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    // Updates the live progress counters as the consumer works through pages.
    public void recordProgress(int processed, int total, String currentTitle) {
        this.processedBooks = processed;
        this.totalBooks = total;
        this.currentBookTitle = currentTitle;
    }

    // Transitions the job to COMPLETED and stamps finishedAt.
    public void markCompleted(int total) {
        this.status = EmbeddingJobStatus.COMPLETED;
        this.totalBooks = total;
        this.processedBooks = total;
        this.currentBookTitle = null;
        this.finishedAt = Instant.now();
    }

    // Transitions the job to FAILED with the given error message and stamps finishedAt.
    public void markFailed(String message) {
        this.status = EmbeddingJobStatus.FAILED;
        this.errorMessage = message;
        this.currentBookTitle = null;
        this.finishedAt = Instant.now();
    }

    // Gets the job id.
    public UUID getId() {
        return id;
    }

    // Gets the job status.
    public EmbeddingJobStatus getStatus() {
        return status;
    }

    // Gets the id of the admin who triggered the job.
    public UUID getTriggeredBy() {
        return triggeredBy;
    }

    // Gets the total number of books in scope for this run.
    public int getTotalBooks() {
        return totalBooks;
    }

    // Gets the number of books processed so far.
    public int getProcessedBooks() {
        return processedBooks;
    }

    // Gets the title of the book currently being processed, if any.
    public String getCurrentBookTitle() {
        return currentBookTitle;
    }

    // Gets the failure error message, if any.
    public String getErrorMessage() {
        return errorMessage;
    }

    // Gets when the job was queued.
    public Instant getQueuedAt() {
        return queuedAt;
    }

    // Gets when the job started running, if it has.
    public Instant getStartedAt() {
        return startedAt;
    }

    // Gets when the job finished, if it has.
    public Instant getFinishedAt() {
        return finishedAt;
    }

    // Entities are equal when their id matches.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EmbeddingJob that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    // Hashes on the id.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
