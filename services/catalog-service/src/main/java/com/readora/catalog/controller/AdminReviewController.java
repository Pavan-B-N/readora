package com.readora.catalog.controller;

import com.readora.catalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Requires the ADMIN role — enforced by UserContextFilter's /api/v1/admin/** gate. */
@Tag(name = "Admin Reviews")
@RestController
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
            summary = "Delete a review",
            description = "Moderation — removes any review regardless of author. Not scoped to a store, since reviews aren't store-specific.",
            tags = {"Admin Reviews"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role"),
            @ApiResponse(responseCode = "404", description = "No such review")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reviewService.deleteReviewAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
