package com.readora.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.dto.BookDoc;
import com.readora.ai.entity.EmbeddingJob;
import com.readora.ai.entity.EmbeddingJobBookLog;
import com.readora.ai.entity.EmbeddingJobStatus;
import com.readora.ai.exception.BackfillAlreadyRunningException;
import com.readora.ai.repository.EmbeddingJobBookLogRepository;
import com.readora.ai.repository.EmbeddingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingJobServiceTest {

    @Mock private EmbeddingJobRepository jobRepository;
    @Mock private EmbeddingJobBookLogRepository bookLogRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private EmbeddingJobService service;
    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EmbeddingJobService(jobRepository, bookLogRepository, embeddingService, kafkaTemplate, new ObjectMapper());
    }

    private static EmbeddingJob job(UUID id) {
        EmbeddingJob job = new EmbeddingJob(UUID.randomUUID());
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    @Test
    void requestBackfill_alreadyActive_throws() {
        when(jobRepository.existsByStatusIn(any())).thenReturn(true);

        assertThatThrownBy(() -> service.requestBackfill(adminId)).isInstanceOf(BackfillAlreadyRunningException.class);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void requestBackfill_noneActive_savesJobAndPublishesEvent() {
        when(jobRepository.existsByStatusIn(any())).thenReturn(false);
        EmbeddingJob saved = job(UUID.randomUUID());
        when(jobRepository.save(any())).thenReturn(saved);

        var response = service.requestBackfill(adminId);

        assertThat(response.id()).isEqualTo(saved.getId());
        verify(kafkaTemplate).send(any(), any(), any());
    }

    @Test
    void runBackfill_jobNotFound_isANoOp() {
        when(jobRepository.findById(any())).thenReturn(Optional.empty());

        service.runBackfill(UUID.randomUUID());

        verify(embeddingService, never()).backfillAll(any());
    }

    @Test
    void runBackfill_jobNotQueued_isANoOp() {
        EmbeddingJob running = job(UUID.randomUUID());
        running.markRunning();
        when(jobRepository.findById(running.getId())).thenReturn(Optional.of(running));

        service.runBackfill(running.getId());

        verify(embeddingService, never()).backfillAll(any());
    }

    @Test
    void runBackfill_queuedJob_runsToCompletion() {
        EmbeddingJob queuedJob = job(UUID.randomUUID());
        when(jobRepository.findById(queuedJob.getId())).thenReturn(Optional.of(queuedJob));
        when(embeddingService.backfillAll(any())).thenReturn(42);

        service.runBackfill(queuedJob.getId());

        assertThat(queuedJob.getStatus()).isEqualTo(EmbeddingJobStatus.COMPLETED);
        assertThat(queuedJob.getTotalBooks()).isEqualTo(42);
    }

    @Test
    void runBackfill_embeddingThrows_marksJobFailed() {
        EmbeddingJob queuedJob = job(UUID.randomUUID());
        when(jobRepository.findById(queuedJob.getId())).thenReturn(Optional.of(queuedJob));
        when(embeddingService.backfillAll(any())).thenThrow(new RuntimeException("embedding API down"));

        service.runBackfill(queuedJob.getId());

        assertThat(queuedJob.getStatus()).isEqualTo(EmbeddingJobStatus.FAILED);
    }

    @Test
    void recordProgress_jobFound_updatesProgressAndLogsEachBook() {
        EmbeddingJob existing = job(UUID.randomUUID());
        when(jobRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        BookDoc book = new BookDoc(UUID.randomUUID().toString(), "Clean Code", List.of(), null, null);

        service.recordProgress(existing.getId(), 1, List.of(book));

        verify(jobRepository).save(existing);
        verify(bookLogRepository).save(any(EmbeddingJobBookLog.class));
    }

    @Test
    void recordProgress_jobNotFound_isANoOp() {
        when(jobRepository.findById(any())).thenReturn(Optional.empty());

        service.recordProgress(UUID.randomUUID(), 1, List.of());

        verify(bookLogRepository, never()).save(any());
    }

    @Test
    void listJobs_mapsRepositoryResults() {
        when(jobRepository.findAllByOrderByQueuedAtDesc(any())).thenReturn(List.of(job(UUID.randomUUID())));

        assertThat(service.listJobs(10)).hasSize(1);
    }

    @Test
    void findActiveJob_none_returnsEmpty() {
        when(jobRepository.findFirstByStatusInOrderByQueuedAtDesc(any())).thenReturn(Optional.empty());

        assertThat(service.findActiveJob()).isEmpty();
    }

    @Test
    void findJob_found_mapsToResponse() {
        EmbeddingJob existing = job(UUID.randomUUID());
        when(jobRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThat(service.findJob(existing.getId())).isPresent();
    }

    @Test
    void listBookLogs_mapsRepositoryResults() {
        when(bookLogRepository.findAllByJobIdOrderByProcessedAtDesc(any())).thenReturn(List.of());

        assertThat(service.listBookLogs(UUID.randomUUID())).isEmpty();
    }
}
