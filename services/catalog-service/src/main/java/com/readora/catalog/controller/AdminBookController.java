package com.readora.catalog.controller;

import com.readora.catalog.dto.AdminBookDetailResponse;
import com.readora.catalog.dto.CreateBookRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.UpdateBookRequest;
import com.readora.catalog.dto.UpdateInventoryRequest;
import com.readora.catalog.dto.UpsertVirtualEditionRequest;
import com.readora.catalog.service.AdminBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin Books")
@RestController
@RequestMapping("/api/v1/admin/books")
public class AdminBookController {

    private final AdminBookService adminBookService;

    public AdminBookController(AdminBookService adminBookService) {
        this.adminBookService = adminBookService;
    }

    @Operation(
            summary = "Get a book for editing",
            description = "Requires the ADMIN role. Returns every field the edit form needs, including ones with no public read use (tableOfContents, coverImageUrl), plus current inventory and virtual-edition state.",
            tags = {"Admin Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book detail returned"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "The book does not exist")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AdminBookDetailResponse> getBookForEdit(@PathVariable UUID id) {
        return ResponseEntity.ok(adminBookService.getBookForEdit(id));
    }

    @Operation(
            summary = "Create a book",
            description = "Requires the ADMIN role. category/publisher are optional; at least one author is required.",
            tags = {"Admin Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "categoryId, publisherId, or one of the authorIds does not exist")
    })
    @PostMapping
    public ResponseEntity<IdResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminBookService.createBook(request));
    }

    @Operation(
            summary = "Update a book",
            description = "Requires the ADMIN role. Every field is applied, not merged — send the current value for anything unchanged. Can also be used to reactivate a deactivated book (isActive: true).",
            tags = {"Admin Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book updated"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "The book, categoryId, publisherId, or one of the authorIds does not exist")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateBook(@PathVariable UUID id, @Valid @RequestBody UpdateBookRequest request) {
        adminBookService.updateBook(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Set stock levels",
            description = "Requires the ADMIN role. Sets the absolute on-hand quantity and reorder threshold — does not touch reserved quantity from open orders.",
            tags = {"Admin Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inventory updated"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "The book does not exist")
    })
    @PutMapping("/{id}/inventory")
    public ResponseEntity<Void> updateInventory(@PathVariable UUID id, @Valid @RequestBody UpdateInventoryRequest request) {
        adminBookService.updateInventory(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Create or update a book's virtual edition",
            description = "Requires the ADMIN role. Upsert — creates the virtual edition if none exists, otherwise updates it (and reactivates it if it had been deactivated).",
            tags = {"Admin Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Virtual edition created or updated"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "The book does not exist")
    })
    @PutMapping("/{id}/virtual-edition")
    public ResponseEntity<Void> upsertVirtualEdition(@PathVariable UUID id, @Valid @RequestBody UpsertVirtualEditionRequest request) {
        adminBookService.upsertVirtualEdition(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Deactivate a book's virtual edition",
            description = "Requires the ADMIN role. No-ops if the book has no virtual edition.",
            tags = {"Admin Books"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Virtual edition deactivated"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @DeleteMapping("/{id}/virtual-edition")
    public ResponseEntity<Void> deactivateVirtualEdition(@PathVariable UUID id) {
        adminBookService.deactivateVirtualEdition(id);
        return ResponseEntity.noContent().build();
    }
}
