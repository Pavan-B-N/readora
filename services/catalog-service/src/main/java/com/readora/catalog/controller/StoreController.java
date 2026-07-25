package com.readora.catalog.controller;

import com.readora.catalog.dto.StoreResponse;
import com.readora.catalog.entity.Store;
import com.readora.catalog.repository.StoreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Public REST endpoint for listing active stores.
@Tag(name = "Stores")
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreRepository storeRepository;

    // Wires in the store repository directly (no service layer needed for this simple read).
    public StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    // Returns every active store a customer can pick from, or an admin can be scoped to.
    @Operation(
            summary = "List active stores",
            description = "Public — every store a customer can pick from, or an admin can be scoped to.",
            tags = {"Stores"}
    )
    @GetMapping
    public ResponseEntity<List<StoreResponse>> listStores() {
        return ResponseEntity.ok(storeRepository.findAllByIsActiveTrueOrderByName().stream().map(this::toResponse).toList());
    }

    // Maps a Store entity to its API response shape.
    private StoreResponse toResponse(Store store) {
        return new StoreResponse(
                store.getId(), store.getName(), store.getCity(), store.getLine1(), store.getLine2(),
                store.getState(), store.getPostalCode(), store.getCountryCode()
        );
    }
}
