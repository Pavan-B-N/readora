package com.readora.catalog.controller;

import com.readora.catalog.dto.AuthorResponse;
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

// Public REST endpoint for listing authors.
@Tag(name = "Authors")
@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final CatalogService catalogService;

    // Wires in the catalog service.
    public AuthorController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    // Returns every author.
    @Operation(
            summary = "List all authors",
            description = "Returns every author. Public — no authentication required.",
            tags = {"Authors"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authors returned")
    })
    @GetMapping
    public ResponseEntity<List<AuthorResponse>> getAll() {
        return ResponseEntity.ok(catalogService.getAllAuthors());
    }
}
