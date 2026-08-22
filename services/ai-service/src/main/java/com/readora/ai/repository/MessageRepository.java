package com.readora.ai.repository;

import com.readora.ai.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findAllByConversationIdOrderByCreatedAt(UUID conversationId);

    long countByConversationId(UUID conversationId);
}
