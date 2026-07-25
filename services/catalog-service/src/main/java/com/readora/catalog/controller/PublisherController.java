package com.readora.catalog.controller;

import com.readora.catalog.dto.PublisherResponse;
import com.readora.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Public REST endpoint for listing publishers.
@Tag(name = "Publishers")
@RestController
@RequestMapping("/api/v1/publishers")
public class PublisherController {

    private final CatalogService catalogService;

    // Wires in the catalog service.
    public PublisherController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // Returns every publisher.
    @Operation(
            summary = "List all publishers",
            description = "Returns every publisher. Public — no authentication required.",
            tags = {"Publishers"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Publishers returned")
    })
    @GetMapping
    public ResponseEntity<List<PublisherResponse>> getAll() {
        return ResponseEntity.ok(catalogService.getAllPublishers());
    }
}
