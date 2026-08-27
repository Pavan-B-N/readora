package com.readora.user.repository;

import com.readora.user.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findAllByUserIdOrderByAddedAtDesc(UUID userId);

    Optional<WishlistItem> findByUserIdAndBookId(UUID userId, UUID bookId);

    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);
}
