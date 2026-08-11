package com.readora.ai.repository;

import com.readora.ai.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// CRUD and lookup access for saved conversations.
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    // Pages a user's conversations, most recently updated first.
    Page<Conversation> findAllByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    // Finds a conversation by id, scoped to its owning user.
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    // Finds the single book-reader conversation for a user and book, if any.
    Optional<Conversation> findByUserIdAndBookId(UUID userId, UUID bookId);
}
