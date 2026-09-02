package com.readora.ai.controller;

import com.readora.ai.dto.EmbeddingJobBookLogResponse;
import com.readora.ai.dto.EmbeddingJobResponse;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.ai.service.EmbeddingJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin Embeddings")
@RestController
@RequestMapping("/api/v1/admin/embeddings")
public class AdminEmbeddingController {

    private final EmbeddingJobService embeddingJobService;

    public AdminEmbeddingController(EmbeddingJobService embeddingJobService) {
        this.embeddingJobService = embeddingJobService;
    }

    @Operation(
            summary = "Queue a full catalogue re-embed",
            description = "Requires the ADMIN role. Publishes a Kafka event and returns immediately with the queued job — the work happens asynchronously in a consumer, since a full backfill calls the embedding API once per page and can run for minutes. Rejected with 409 if a backfill is already queued or running, because concurrent runs would duplicate expensive API calls for no benefit. Poll the returned job for progress.",
            tags = {"Admin Embeddings"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Backfill queued"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "409", description = "A backfill is already queued or running")
    })
    @PostMapping("/backfill")
    public ResponseEntity<EmbeddingJobResponse> queueBackfill() {
        EmbeddingJobResponse job = embeddingJobService.requestBackfill(CurrentUserContext.require());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @Operation(
            summary = "List recent backfill jobs",
            description = "Requires the ADMIN role. Returns backfill history, newest first, with status, progress counters, and timings.",
            tags = {"Admin Embeddings"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job history returned"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @GetMapping("/jobs")
    public ResponseEntity<List<EmbeddingJobResponse>> listJobs(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(embeddingJobService.listJobs(limit));
    }

    @Operation(
            summary = "Get one backfill job",
            description = "Requires the ADMIN role. Poll this while a job is RUNNING to track live progress.",
            tags = {"Admin Embeddings"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job returned"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "No such job")
    })
    @GetMapping("/jobs/{id}")
    public ResponseEntity<EmbeddingJobResponse> getJob(@PathVariable UUID id) {
        return embeddingJobService.findJob(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "List the books a backfill job has embedded so far",
            description = "Requires the ADMIN role. Newest first. Poll this alongside the job itself while it's RUNNING for a live, book-by-book feed instead of just the aggregate counters.",
            tags = {"Admin Embeddings"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book log returned (possibly empty)"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @GetMapping("/jobs/{id}/books")
    public ResponseEntity<List<EmbeddingJobBookLogResponse>> listJobBooks(@PathVariable UUID id) {
        return ResponseEntity.ok(embeddingJobService.listBookLogs(id));
    }
}
