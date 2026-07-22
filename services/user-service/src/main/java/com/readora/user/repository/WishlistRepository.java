package com.readora.user.repository;

import com.readora.user.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Spring Data repository for wishlist items.
public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {

    // A user's wishlist, newest-first.
    List<WishlistItem> findAllByUserIdOrderByAddedAtDesc(UUID userId);

    // Looks up a wishlist entry for a (user, book) pair.
    Optional<WishlistItem> findByUserIdAndBookId(UUID userId, UUID bookId);

    // Whether a book is on a user's wishlist.
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);
}
