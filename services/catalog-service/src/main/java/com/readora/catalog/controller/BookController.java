package com.readora.catalog.controller;

import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.BookSuggestionResponse;
import com.readora.catalog.dto.BookSummaryResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.RelatedBookResponse;
import com.readora.catalog.security.CurrentUserContext;
import com.readora.catalog.service.CatalogService;
import com.readora.catalog.service.VirtualContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Books")
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final CatalogService catalogService;
    private final VirtualContentService virtualContentService;

    public BookController(CatalogService catalogService, VirtualContentService virtualContentService) {
        this.catalogService = catalogService;
        this.virtualContentService = virtualContentService;
    }

    @Operation(
            summary = "Search the catalogue",
            description = "Searches and filters books by free-text query, category, publisher, and price range. virtualOnly=false (default) is the \"Physical\" tab — books with a store, optionally further scoped to storeId; virtualOnly=true is the \"Virtual editions\" tab — books with an active virtual edition, ignoring store (and storeId) entirely. Public — no authentication required.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated book results returned")
    })
    @GetMapping
    public ResponseEntity<PageResponse<BookSummaryResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID publisherId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean virtualOnly,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(catalogService.search(q, categoryId, publisherId, minPrice, maxPrice, virtualOnly, storeId, pageable));
    }

    @Operation(
            summary = "Typeahead suggestions for the search bar",
            description = "Top title matches for a partial query, capped at 10. Public — no authentication required. Deliberately a plain substring match, not ai-service's semantic search — see CatalogService.suggest's javadoc for why.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Suggestions returned (possibly empty)")
    })
    @GetMapping("/suggest")
    public ResponseEntity<List<BookSuggestionResponse>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int limit
    ) {
        return ResponseEntity.ok(catalogService.suggest(q, limit));
    }

    @Operation(
            summary = "Get personalized recommendations",
            description = "Books from the same categories as the caller's past purchases, excluding titles already owned. Empty for anonymous callers or callers with no purchase history — never an error.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommended books returned (possibly empty)")
    })
    @GetMapping("/recommended")
    public ResponseEntity<List<BookSummaryResponse>> recommended() {
        UUID userId = CurrentUserContext.get().orElse(null);
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(catalogService.getRecommendations(userId));
    }

    @Operation(
            summary = "Get book detail",
            description = "Returns full detail for one book, including live availability. Public — no authentication required.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book detail returned"),
            @ApiResponse(responseCode = "404", description = "No active book with that id")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookDetailResponse> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getDetail(id));
    }

    @Operation(
            summary = "Get related titles",
            description = "Returns cross-sell titles related to one book. Public — no authentication required.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Related titles returned"),
            @ApiResponse(responseCode = "404", description = "The parent book does not exist")
    })
    @GetMapping("/{id}/related")
    public ResponseEntity<List<RelatedBookResponse>> getRelated(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getRelated(id));
    }

    @Operation(
            summary = "Read a virtual edition in-app",
            description = "Streams the virtual edition's file for in-app viewing only — never a downloadable link. Requires the caller to have purchased this book.",
            tags = {"Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File stream begins"),
            @ApiResponse(responseCode = "403", description = "The caller hasn't purchased this book"),
            @ApiResponse(responseCode = "404", description = "No active virtual edition for this book")
    })
    @GetMapping("/{id}/read")
    public ResponseEntity<Resource> read(@PathVariable UUID id) {
        Resource content = virtualContentService.getContent(CurrentUserContext.require(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(content);
    }
}
