package com.readora.catalog.controller;

import com.readora.catalog.dto.BookExportPage;
import com.readora.catalog.dto.VirtualEditionLookupRequest;
import com.readora.catalog.dto.VirtualEditionLookupResponse;
import com.readora.catalog.service.InternalCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal")
public class InternalCatalogController {

    private final InternalCatalogService internalCatalogService;

    public InternalCatalogController(InternalCatalogService internalCatalogService) {
        this.internalCatalogService = internalCatalogService;
    }

    @Operation(
            summary = "Export book text for embedding",
            description = "Internal, service-to-service only. Called by ai-service's embedding backfill to pull title, authors, description, and table of contents for every book.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of books returned")
    })
    @GetMapping("/books/export")
    public ResponseEntity<BookExportPage> exportBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(internalCatalogService.exportBooks(pageable));
    }

    @Operation(
            summary = "Look up virtual edition availability and price",
            description = "Internal, service-to-service only. Called by commerce-service during virtual checkout to confirm every requested book has an active virtual edition, and to price it.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability and price returned for every requested book id")
    })
    @PostMapping("/virtual-editions/lookup")
    public ResponseEntity<VirtualEditionLookupResponse> lookupVirtualEditions(@RequestBody VirtualEditionLookupRequest request) {
        return ResponseEntity.ok(internalCatalogService.lookupVirtualEditions(request.bookIds()));
    }
}
