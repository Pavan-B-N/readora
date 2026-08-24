package com.readora.ai.controller;

import com.readora.ai.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Embeddings")
@RestController
@RequestMapping("/api/v1/admin/embeddings")
public class AdminEmbeddingController {

    private final EmbeddingService embeddingService;

    public AdminEmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @Operation(
            summary = "Re-embed the entire catalog",
            description = "Requires the ADMIN role. Re-embeds every book from catalog-service into the vector store, replacing existing vectors for the same book ids. Runs synchronously, so for a large catalog the request may take a while — day-to-day updates flow through the book.upserted Kafka event instead, this is for bootstrapping or recovery.",
            tags = {"Admin Embeddings"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Backfill completed"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PostMapping("/backfill")
    public ResponseEntity<Void> backfill() {
        embeddingService.backfillAll();
        return ResponseEntity.noContent().build();
    }
}
