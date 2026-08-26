package com.readora.catalog.repository;

import com.readora.catalog.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findAllByBook_IdOrderByCreatedAtDesc(UUID bookId, Pageable pageable);

    Optional<Review> findByBook_IdAndUserId(UUID bookId, UUID userId);

    void deleteByBook_IdAndUserId(UUID bookId, UUID userId);

    /** Projection avoids a clunky Object[] cast — Spring Data maps the aliased columns straight onto it. */
    interface RatingAggregate {
        Double getAverageRating();

        long getReviewCount();
    }

    @Query("SELECT AVG(r.rating) AS averageRating, COUNT(r) AS reviewCount FROM Review r WHERE r.book.id = :bookId")
    RatingAggregate getAggregateForBook(@Param("bookId") UUID bookId);
}
