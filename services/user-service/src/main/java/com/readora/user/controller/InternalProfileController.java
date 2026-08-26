package com.readora.user.controller;

import com.readora.user.dto.DisplayNameResponse;
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
@RequestMapping("/internal/profiles")
public class InternalProfileController {

    private final UserService userService;

    public InternalProfileController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get a user's display name",
            description = "Internal, service-to-service only — protected by the shared gateway secret. Called by catalog-service to snapshot a display name onto a review at write time.",
            tags = {"Internal"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "displayName returned (null if no profile or none set)")
    })
    @GetMapping("/{userId}/display-name")
    public ResponseEntity<DisplayNameResponse> getDisplayName(@PathVariable UUID userId) {
        return ResponseEntity.ok(new DisplayNameResponse(userService.getDisplayName(userId)));
    }
}
