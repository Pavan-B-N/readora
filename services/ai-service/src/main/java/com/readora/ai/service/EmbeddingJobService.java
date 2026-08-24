package com.readora.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.dto.EmbeddingBackfillRequestedEvent;
import com.readora.ai.dto.EmbeddingJobResponse;
import com.readora.ai.entity.EmbeddingJob;
import com.readora.ai.entity.EmbeddingJobStatus;
import com.readora.ai.exception.BackfillAlreadyRunningException;
import com.readora.ai.kafka.KafkaTopics;
import com.readora.ai.repository.EmbeddingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Backfills run asynchronously off a Kafka event rather than inline in the HTTP request: a full
 * re-embed calls an external embedding API once per page and can take minutes, which is far too
 * long to hold a connection open. The request only enqueues a job and returns its id; the
 * consumer does the work and writes progress back so the UI can poll.
 */
@Service
public class EmbeddingJobService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingJobService.class);
    private static final List<EmbeddingJobStatus> ACTIVE = List.of(EmbeddingJobStatus.QUEUED, EmbeddingJobStatus.RUNNING);

    private final EmbeddingJobRepository jobRepository;
    private final EmbeddingService embeddingService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EmbeddingJobService(
            EmbeddingJobRepository jobRepository,
            EmbeddingService embeddingService,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.embeddingService = embeddingService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Queues a backfill and publishes the event that triggers it.
     *
     * @param triggeredBy the admin requesting the backfill
     * @return the queued job
     * @throws BackfillAlreadyRunningException if one is already queued or running — concurrent
     *         backfills would duplicate expensive embedding-API calls for no benefit
     */
    @Transactional
    public EmbeddingJobResponse requestBackfill(UUID triggeredBy) {
        if (jobRepository.existsByStatusIn(ACTIVE)) {
            throw new BackfillAlreadyRunningException();
        }

        EmbeddingJob job = jobRepository.save(new EmbeddingJob(triggeredBy));

        try {
            String payload = objectMapper.writeValueAsString(new EmbeddingBackfillRequestedEvent(job.getId()));
            kafkaTemplate.send(KafkaTopics.EMBEDDING_BACKFILL_REQUESTED, job.getId().toString(), payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize backfill event", e);
        }

        return toResponse(job);
    }

    /**
     * Runs a queued backfill to completion. Called by the Kafka consumer, never directly by a
     * request thread.
     *
     * @param jobId the job to run
     */
    public void runBackfill(UUID jobId) {
        EmbeddingJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != EmbeddingJobStatus.QUEUED) {
            // Already picked up (Kafka redelivery) or unknown — nothing to do.
            return;
        }

        updateStatus(jobId, EmbeddingJob::markRunning);

        try {
            int total = embeddingService.backfillAll(
                    (processed, title) -> recordProgress(jobId, processed, title)
            );
            updateStatus(jobId, j -> j.markCompleted(total));
            log.info("Embedding backfill {} completed — {} books embedded", jobId, total);
        } catch (Exception e) {
            log.error("Embedding backfill {} failed", jobId, e);
            updateStatus(jobId, j -> j.markFailed(rootMessage(e)));
        }
    }

    /**
     * Progress updates commit in their own transaction so the admin UI can poll and see them
     * while the (long-running) backfill is still in flight.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProgress(UUID jobId, int processed, String currentTitle) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.recordProgress(processed, Math.max(processed, job.getTotalBooks()), currentTitle);
            jobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(UUID jobId, java.util.function.Consumer<EmbeddingJob> mutation) {
        jobRepository.findById(jobId).ifPresent(job -> {
            mutation.accept(job);
            jobRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public List<EmbeddingJobResponse> listJobs(int limit) {
        return jobRepository.findAllByOrderByQueuedAtDesc(PageRequest.of(0, limit)).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EmbeddingJobResponse> findActiveJob() {
        return jobRepository.findFirstByStatusInOrderByQueuedAtDesc(ACTIVE).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<EmbeddingJobResponse> findJob(UUID jobId) {
        return jobRepository.findById(jobId).map(this::toResponse);
    }

    private EmbeddingJobResponse toResponse(EmbeddingJob job) {
        return new EmbeddingJobResponse(
                job.getId(), job.getStatus(), job.getTotalBooks(), job.getProcessedBooks(),
                job.getCurrentBookTitle(), job.getErrorMessage(),
                job.getQueuedAt(), job.getStartedAt(), job.getFinishedAt()
        );
    }

    private String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null && message.length() > 500 ? message.substring(0, 500) : message;
    }
}
