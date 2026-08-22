package com.readora.catalog.controller;

import com.readora.catalog.dto.ReserveStockRequest;
import com.readora.catalog.dto.ReserveStockResponse;
import com.readora.catalog.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/inventory")
public class InternalInventoryController {

    private final InventoryService inventoryService;

    public InternalInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(
            summary = "Reserve stock for a checkout",
            description = "Internal, service-to-service only — not part of the public API surface, protected by the shared gateway secret rather than a user's JWT. Called by commerce-service during checkout to atomically check and reserve stock for every item before an order is persisted.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All items reserved successfully"),
            @ApiResponse(responseCode = "404", description = "One of the requested books does not exist or is inactive"),
            @ApiResponse(responseCode = "409", description = "One of the requested books does not have enough available stock")
    })
    @PostMapping("/reserve")
    public ResponseEntity<ReserveStockResponse> reserve(@Valid @RequestBody ReserveStockRequest request) {
        return ResponseEntity.ok(inventoryService.reserve(request));
    }
}
