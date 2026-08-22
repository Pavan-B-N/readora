package com.readora.catalog.controller;

import com.readora.catalog.dto.CategoryResponse;
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

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CatalogService catalogService;

    public CategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(
            summary = "Get the category tree",
            description = "Returns the full category tree (parents with nested children). Public — no authentication required.",
            tags = {"Categories"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category tree returned")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getTree() {
        return ResponseEntity.ok(catalogService.getCategoryTree());
    }
}
