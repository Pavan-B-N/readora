package com.readora.ai.controller;

import com.readora.ai.dto.EmbeddingJobResponse;
import com.readora.ai.entity.EmbeddingJobStatus;
import com.readora.ai.security.CurrentUserContext;
import com.readora.ai.service.EmbeddingJobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEmbeddingControllerTest {

    @Mock private EmbeddingJobService embeddingJobService;

    private AdminEmbeddingController controller;

    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new AdminEmbeddingController(embeddingJobService);
        CurrentUserContext.set(adminId, List.of("ADMIN"));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private static EmbeddingJobResponse job(UUID id) {
        return new EmbeddingJobResponse(id, EmbeddingJobStatus.QUEUED, 0, 0, null, null, null, null, null);
    }

    @Test
    void queueBackfill_returns202WithTheQueuedJob() {
        EmbeddingJobResponse response = job(UUID.randomUUID());
        when(embeddingJobService.requestBackfill(adminId)).thenReturn(response);

        var result = controller.queueBackfill();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void listJobs_delegatesToServiceWithTheGivenLimit() {
        when(embeddingJobService.listJobs(5)).thenReturn(List.of(job(UUID.randomUUID())));

        var result = controller.listJobs(5);

        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void getJob_found_returns200() {
        UUID id = UUID.randomUUID();
        when(embeddingJobService.findJob(id)).thenReturn(Optional.of(job(id)));

        var result = controller.getJob(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getJob_notFound_returns404() {
        UUID id = UUID.randomUUID();
        when(embeddingJobService.findJob(id)).thenReturn(Optional.empty());

        var result = controller.getJob(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listJobBooks_delegatesToService() {
        UUID id = UUID.randomUUID();
        when(embeddingJobService.listBookLogs(id)).thenReturn(List.of());

        var result = controller.listJobBooks(id);

        assertThat(result.getBody()).isEmpty();
    }
}
