package com.readora.catalog.controller;

import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.ReviewResponse;
import com.readora.catalog.dto.UpsertReviewRequest;
import com.readora.catalog.security.CurrentUserContext;
import com.readora.catalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Reviews")
@RestController
@RequestMapping("/api/v1/books/{bookId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
            summary = "List a book's reviews",
            description = "Newest first. Public — no authentication required.",
            tags = {"Reviews"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated reviews returned")
    })
    @GetMapping
    public ResponseEntity<PageResponse<ReviewResponse>> list(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(reviewService.getReviews(bookId, pageable));
    }

    @Operation(
            summary = "Add or update the caller's review for this book",
            description = "One review per (book, caller) — resubmitting updates the existing review rather than adding a duplicate.",
            tags = {"Reviews"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review saved"),
            @ApiResponse(responseCode = "404", description = "The book does not exist")
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> upsert(@PathVariable UUID bookId, @Valid @RequestBody UpsertReviewRequest request) {
        return ResponseEntity.ok(reviewService.upsertReview(CurrentUserContext.require(), bookId, request));
    }

    @Operation(
            summary = "Delete the caller's own review for this book",
            tags = {"Reviews"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted (no-ops if none existed)")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteOwn(@PathVariable UUID bookId) {
        reviewService.deleteOwnReview(CurrentUserContext.require(), bookId);
        return ResponseEntity.noContent().build();
    }
}
