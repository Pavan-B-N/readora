package com.readora.ai.controller;

import com.readora.ai.dto.SearchResponse;
import com.readora.ai.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Search")
@RestController
@RequestMapping("/api/v1/ai")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(
            summary = "Semantic catalogue search",
            description = "Embeds the query and returns the nearest books by cosine similarity. No LLM call, no agent loop — pure retrieval, fast enough to sit behind the normal search box. Public — no authentication required.",
            tags = {"AI Search"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned"),
            @ApiResponse(responseCode = "400", description = "Missing query, or limit above 50")
    })
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(searchService.search(q, Math.min(limit, 50)));
    }
}
