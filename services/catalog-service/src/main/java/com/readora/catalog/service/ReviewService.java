package com.readora.catalog.service;

import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.client.UserServiceClient;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.ReviewResponse;
import com.readora.catalog.dto.UpsertReviewRequest;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Review;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.ReviewNotFoundException;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// Anyone authenticated can review a book — purchase isn't required, only flagged as "Verified Purchase," the more common real-world pattern anyway (and easier to exercise without real payment/moderation infrastructure).
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserServiceClient userServiceClient;
    private final CommerceClient commerceClient;

    // Wires in the repositories and clients this service depends on.
    public ReviewService(
            ReviewRepository reviewRepository,
            BookRepository bookRepository,
            UserServiceClient userServiceClient,
            CommerceClient commerceClient
    ) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userServiceClient = userServiceClient;
        this.commerceClient = commerceClient;
    }

    // Returns a book's reviews, newest first.
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(UUID bookId, Pageable pageable) {
        Page<Review> page = reviewRepository.findAllByBook_IdOrderByCreatedAtDesc(bookId, pageable);
        List<ReviewResponse> items = page.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /** One review per (book, user) — resubmitting updates the existing row rather than creating a duplicate. */
    @Transactional
    public ReviewResponse upsertReview(UUID userId, UUID bookId, UpsertReviewRequest request) {
        boolean verifiedPurchase = commerceClient.getPurchasedBookIds(userId).contains(bookId);

        Review review = reviewRepository.findByBook_IdAndUserId(bookId, userId).orElse(null);
        if (review != null) {
            review.update(request.rating(), request.comment(), verifiedPurchase);
        } else {
            Book book = bookRepository.findById(bookId).filter(Book::isActive).orElseThrow(BookNotFoundException::new);
            String displayName = userServiceClient.getDisplayName(userId);
            review = new Review(book, userId, displayName, request.rating(), request.comment(), verifiedPurchase);
        }

        reviewRepository.save(review);
        return toResponse(review);
    }

    // Deletes the caller's own review of a book, no-oping if none exists.
    @Transactional
    public void deleteOwnReview(UUID userId, UUID bookId) {
        reviewRepository.deleteByBook_IdAndUserId(bookId, userId);
    }

    // Deletes any review as a moderation action.
    @Transactional
    public void deleteReviewAsAdmin(UUID reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ReviewNotFoundException();
        }
        reviewRepository.deleteById(reviewId);
    }

    // Maps a Review entity to its API response shape, defaulting a missing author name to "Anonymous".
    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(), review.getUserId(),
                review.getAuthorDisplayName() != null ? review.getAuthorDisplayName() : "Anonymous",
                review.getRating(), review.getComment(), review.isVerifiedPurchase(), review.getCreatedAt()
        );
    }
}
