package com.readora.user.controller;

import com.readora.user.dto.AdminStoreResponse;
import com.readora.user.dto.StoreAdminResponse;
import com.readora.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Internal")
@RestController
@RequestMapping("/internal/admin-users")
public class InternalAdminController {

    private final UserService userService;

    public InternalAdminController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get the store an admin is assigned to manage",
            description = "Internal, service-to-service only — protected by the shared gateway secret, not a user's JWT. Called by catalog-service to enforce that an admin's book-management requests stay scoped to their own store, server-side.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "storeId returned (null if unassigned or not an admin)")
    })
    @GetMapping("/{userId}/store")
    public ResponseEntity<AdminStoreResponse> getAdminStore(@PathVariable UUID userId) {
        return ResponseEntity.ok(new AdminStoreResponse(userService.getAdminStoreId(userId)));
    }

    @Operation(
            summary = "Get the admin assigned to manage a store",
            description = "Internal, service-to-service only. The reverse of GET /{userId}/store — called by commerce-service to find who to notify about a return at that store.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "userId returned (null if the store has no assigned admin)")
    })
    @GetMapping("/by-store/{storeId}")
    public ResponseEntity<StoreAdminResponse> getStoreAdmin(@PathVariable UUID storeId) {
        return ResponseEntity.ok(new StoreAdminResponse(userService.getAdminUserIdForStore(storeId)));
    }
}
