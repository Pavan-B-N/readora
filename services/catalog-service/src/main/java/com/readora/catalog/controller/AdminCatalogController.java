package com.readora.catalog.controller;

import com.readora.catalog.dto.CreateAuthorRequest;
import com.readora.catalog.dto.CreateCategoryRequest;
import com.readora.catalog.dto.CreatePublisherRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.service.AdminCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Catalog")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    public AdminCatalogController(AdminCatalogService adminCatalogService) {
        this.adminCatalogService = adminCatalogService;
    }

    @Operation(
            summary = "Create a category",
            description = "Requires the ADMIN role. Creates a category, optionally nested under a parent.",
            tags = {"Admin Catalog"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "parentId does not exist")
    })
    @PostMapping("/categories")
    public ResponseEntity<IdResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCatalogService.createCategory(request));
    }

    @Operation(
            summary = "Create a publisher",
            description = "Requires the ADMIN role.",
            tags = {"Admin Catalog"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publisher created"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PostMapping("/publishers")
    public ResponseEntity<IdResponse> createPublisher(@Valid @RequestBody CreatePublisherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCatalogService.createPublisher(request));
    }

    @Operation(
            summary = "Create an author",
            description = "Requires the ADMIN role.",
            tags = {"Admin Catalog"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Author created"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PostMapping("/authors")
    public ResponseEntity<IdResponse> createAuthor(@Valid @RequestBody CreateAuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCatalogService.createAuthor(request));
    }
}
