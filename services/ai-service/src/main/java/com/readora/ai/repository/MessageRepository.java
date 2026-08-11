package com.readora.ai.repository;

import com.readora.ai.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// CRUD and lookup access for conversation messages.
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Lists a conversation's messages in chronological order.
    List<Message> findAllByConversationIdOrderByCreatedAt(UUID conversationId);

    // Counts how many messages a conversation has.
    long countByConversationId(UUID conversationId);
}
