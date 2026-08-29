package com.readora.catalog.controller;

import com.readora.catalog.dto.BookAvailabilityRequest;
import com.readora.catalog.dto.BookAvailabilityResponse;
import com.readora.catalog.dto.BookCoverLookupRequest;
import com.readora.catalog.dto.BookCoverLookupResponse;
import com.readora.catalog.dto.BookExportPage;
import com.readora.catalog.dto.BookLookupRequest;
import com.readora.catalog.dto.BookLookupResponse;
import com.readora.catalog.dto.MarkEmbeddedRequest;
import com.readora.catalog.dto.StoreResponse;
import com.readora.catalog.dto.VirtualEditionLookupRequest;
import com.readora.catalog.dto.VirtualEditionLookupResponse;
import com.readora.catalog.service.InternalCatalogService;
import com.readora.catalog.service.VirtualContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal")
public class InternalCatalogController {

    private final InternalCatalogService internalCatalogService;
    private final VirtualContentService virtualContentService;

    public InternalCatalogController(InternalCatalogService internalCatalogService, VirtualContentService virtualContentService) {
        this.internalCatalogService = internalCatalogService;
        this.virtualContentService = virtualContentService;
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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "false") boolean needsReembeddingOnly
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(internalCatalogService.exportBooks(pageable, needsReembeddingOnly));
    }

    @Operation(
            summary = "Mark books as embedded",
            description = "Internal, service-to-service only. Called by ai-service after it successfully embeds a batch of books, so a later backfill run skips them unless they change again.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Books marked embedded as of now")
    })
    @PostMapping("/books/embedded")
    public ResponseEntity<Void> markEmbedded(@RequestBody MarkEmbeddedRequest request) {
        internalCatalogService.markEmbedded(request.bookIds());
        return ResponseEntity.noContent().build();
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

    @Operation(
            summary = "Look up book text for specific ids",
            description = "Internal, service-to-service only. Called by ai-service's incremental embedding consumer to re-embed one changed book without paginating the whole catalog.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Books found for the requested ids (missing ids are silently omitted)")
    })
    @PostMapping("/books/lookup")
    public ResponseEntity<BookLookupResponse> lookupBooks(@RequestBody BookLookupRequest request) {
        return ResponseEntity.ok(internalCatalogService.lookupBooks(request.bookIds()));
    }

    @Operation(
            summary = "Filter book ids down to ones actually purchasable at a store",
            description = "Internal, service-to-service only. Called by ai-service's book-recommendation tools so "
                    + "the assistant never recommends a title with no virtual edition and no stock at the caller's store.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The subset of the requested ids that are actually purchasable")
    })
    @PostMapping("/books/availability")
    public ResponseEntity<BookAvailabilityResponse> checkAvailability(@RequestBody BookAvailabilityRequest request) {
        return ResponseEntity.ok(internalCatalogService.checkAvailability(request.bookIds(), request.storeId()));
    }

    @Operation(
            summary = "Look up cover images for specific book ids",
            description = "Internal, service-to-service only. Called by commerce-service to render book-cover thumbnails on the order list without duplicating cover-image storage.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Covers found for the requested ids (missing ids are silently omitted)")
    })
    @PostMapping("/books/covers")
    public ResponseEntity<BookCoverLookupResponse> lookupCovers(@RequestBody BookCoverLookupRequest request) {
        return ResponseEntity.ok(internalCatalogService.lookupCovers(request.bookIds()));
    }

    @Operation(
            summary = "Look up a store by id",
            description = "Internal, service-to-service only. Called by commerce-service during checkout to validate "
                    + "a shipping address's city against the store the order is delivering from.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Store found"),
            @ApiResponse(responseCode = "404", description = "No store with that id")
    })
    @GetMapping("/stores/{storeId}")
    public ResponseEntity<StoreResponse> findStore(@PathVariable UUID storeId) {
        return ResponseEntity.ok(internalCatalogService.findStore(storeId));
    }

    @Operation(
            summary = "Check whether a user owns a book's virtual edition",
            description = "Internal, service-to-service only. Called by ai-service before indexing or chatting "
                    + "about a book's content, so only a real purchaser can trigger embedding work or read the assistant's answers.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ownership boolean returned")
    })
    @GetMapping("/books/{bookId}/owned")
    public ResponseEntity<OwnedResponse> isOwned(@PathVariable UUID bookId, @RequestParam UUID userId) {
        return ResponseEntity.ok(new OwnedResponse(virtualContentService.isOwned(userId, bookId)));
    }

    @Operation(
            summary = "Fetch a virtual edition's raw file",
            description = "Internal, service-to-service only. Called by ai-service's reader-indexing pipeline to "
                    + "extract and embed a book's text. No ownership check here — the caller already verified "
                    + "ownership via the /owned endpoint above; this just resolves the stored file.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File streamed")
    })
    @GetMapping("/books/{bookId}/content")
    public ResponseEntity<Resource> getContent(@PathVariable UUID bookId) {
        return ResponseEntity.ok(virtualContentService.getContentForInternalUse(bookId));
    }

    private record OwnedResponse(boolean owned) {
    }
}
