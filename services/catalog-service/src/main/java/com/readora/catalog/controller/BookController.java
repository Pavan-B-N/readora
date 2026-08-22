package com.readora.catalog.controller;

import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.BookSummaryResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.RelatedBookResponse;
import com.readora.catalog.entity.BookFormat;
import com.readora.catalog.service.CatalogService;
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

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final CatalogService catalogService;

    public BookController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<BookDetailResponse> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getDetail(id));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<RelatedBookResponse>> getRelated(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getRelated(id));
    }
}
