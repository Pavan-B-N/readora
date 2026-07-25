package com.readora.catalog.repository;

import com.readora.catalog.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// Spring Data JPA repository for Review.
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Returns a book's reviews newest-first.
    Page<Review> findAllByBook_IdOrderByCreatedAtDesc(UUID bookId, Pageable pageable);

    // Returns a user's review of a book, if any.
    Optional<Review> findByBook_IdAndUserId(UUID bookId, UUID userId);

    // Deletes a user's review of a book.
    void deleteByBook_IdAndUserId(UUID bookId, UUID userId);

    // Projection avoids a clunky Object[] cast — Spring Data maps the aliased columns straight onto it.
    interface RatingAggregate {
        // Returns the average rating.
        Double getAverageRating();

        // Returns the number of reviews.
        long getReviewCount();
    }

    // Returns the average rating and review count for a book in one query.
    @Query("SELECT AVG(r.rating) AS averageRating, COUNT(r) AS reviewCount FROM Review r WHERE r.book.id = :bookId")
    RatingAggregate getAggregateForBook(@Param("bookId") UUID bookId);
}
