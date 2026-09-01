package com.readora.catalog.service;

import com.readora.catalog.client.CommerceClient;
import com.readora.catalog.client.UserServiceClient;
import com.readora.catalog.dto.UpsertReviewRequest;
import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.Review;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.ReviewNotFoundException;
import com.readora.catalog.repository.BookRepository;
import com.readora.catalog.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookRepository bookRepository;
    @Mock private UserServiceClient userServiceClient;
    @Mock private CommerceClient commerceClient;

    private ReviewService reviewService;
    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, bookRepository, userServiceClient, commerceClient);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void upsertReview_newReview_notPurchased_marksUnverified() throws Exception {
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of());
        when(reviewRepository.findByBook_IdAndUserId(bookId, userId)).thenReturn(Optional.empty());
        Book book = new Book("9780000000001", "T", "D", null, null, null, "en", 1, null,
                BigDecimal.TEN, "INR", null, null);
        setField(book, "id", bookId);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(userServiceClient.getDisplayName(userId)).thenReturn("A Reader");

        var response = reviewService.upsertReview(userId, bookId, new UpsertReviewRequest(4, "Good"));

        assertThat(response.verifiedPurchase()).isFalse();
        assertThat(response.rating()).isEqualTo(4);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void upsertReview_newReview_bookNotFound_throws() {
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of());
        when(reviewRepository.findByBook_IdAndUserId(bookId, userId)).thenReturn(Optional.empty());
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.upsertReview(userId, bookId, new UpsertReviewRequest(4, "Good")))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void upsertReview_existingReview_updatesInPlaceRatherThanCreatingDuplicate() throws Exception {
        when(commerceClient.getPurchasedBookIds(userId)).thenReturn(List.of(bookId));
        Book book = new Book("9780000000001", "T", "D", null, null, null, "en", 1, null,
                BigDecimal.TEN, "INR", null, null);
        Review existing = new Review(book, userId, "A Reader", 2, "Meh", false);
        when(reviewRepository.findByBook_IdAndUserId(bookId, userId)).thenReturn(Optional.of(existing));

        var response = reviewService.upsertReview(userId, bookId, new UpsertReviewRequest(5, "Great now"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.verifiedPurchase()).isTrue();
        verify(bookRepository, never()).findById(any());
        verify(reviewRepository).save(existing);
    }

    @Test
    void deleteOwnReview_delegatesToRepository() {
        reviewService.deleteOwnReview(userId, bookId);

        verify(reviewRepository).deleteByBook_IdAndUserId(bookId, userId);
    }

    @Test
    void deleteReviewAsAdmin_notFound_throws() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.existsById(reviewId)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.deleteReviewAsAdmin(reviewId)).isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void deleteReviewAsAdmin_found_deletes() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.existsById(reviewId)).thenReturn(true);

        reviewService.deleteReviewAsAdmin(reviewId);

        verify(reviewRepository).deleteById(reviewId);
    }

    @Test
    void getReviews_mapsPageContent() {
        Book book = org.mockito.Mockito.mock(Book.class);
        Review review = new Review(book, userId, null, 3, "ok", false);
        when(reviewRepository.findAllByBook_IdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(review)));

        var response = reviewService.getReviews(bookId, org.springframework.data.domain.Pageable.unpaged());

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).authorDisplayName()).isEqualTo("Anonymous");
    }
}
