package com.readora.catalog.controller;

import com.readora.catalog.dto.CreateAuthorRequest;
import com.readora.catalog.dto.CreateCategoryRequest;
import com.readora.catalog.dto.CreatePublisherRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.UpdateAuthorRequest;
import com.readora.catalog.dto.UpdateCategoryRequest;
import com.readora.catalog.service.AdminCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
            summary = "Update a category",
            description = "Requires the ADMIN role. Every field is applied, not merged.",
            tags = {"Admin Catalog"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category updated"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "No such category")
    })
    @PutMapping("/categories/{id}")
    public ResponseEntity<Void> updateCategory(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        adminCatalogService.updateCategory(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a category",
            description = "Requires the ADMIN role. Blocked while any book still references it.",
            tags = {"Admin Catalog"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "No such category"),
            @ApiResponse(responseCode = "409", description = "One or more books still reference this category")
    })
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        adminCatalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
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

    @Operation(
            summary = "Update an author",
            description = "Requires the ADMIN role. Every field is applied, not merged.",
            tags = {"Admin Catalog"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Author updated"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "No such author")
    })
    @PutMapping("/authors/{id}")
    public ResponseEntity<Void> updateAuthor(@PathVariable UUID id, @Valid @RequestBody UpdateAuthorRequest request) {
        adminCatalogService.updateAuthor(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete an author",
            description = "Requires the ADMIN role. Blocked while any book still credits this author.",
            tags = {"Admin Catalog"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Author deleted"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "No such author"),
            @ApiResponse(responseCode = "409", description = "One or more books still credit this author")
    })
    @DeleteMapping("/authors/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable UUID id) {
        adminCatalogService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }
}
