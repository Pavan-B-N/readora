package com.readora.catalog.controller;

import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.BookSummaryResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.RelatedBookResponse;
import com.readora.catalog.entity.BookFormat;
import com.readora.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public BookController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(
            summary = "Search the catalogue",
            description = "Searches and filters books by free-text query, category, publisher, format, and price range. Public — no authentication required.",
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
            @RequestParam(required = false) BookFormat format,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(catalogService.search(q, categoryId, publisherId, format, minPrice, maxPrice, pageable));
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
}
