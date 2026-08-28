package com.readora.ai.service;

import com.readora.ai.dto.ConversationSummaryResponse;
import com.readora.ai.dto.MessageResponse;
import com.readora.ai.entity.Message;
import com.readora.ai.exception.ConversationNotFoundException;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public Page<ConversationSummaryResponse> list(UUID userId, Pageable pageable) {
        return conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(c -> new ConversationSummaryResponse(
                        c.getId(), c.getTitle(), messageRepository.countByConversationId(c.getId()), c.getUpdatedAt()
                ));
    }

    /** Restores a conversation's turn-by-turn history — used to resume a chat on reopen rather than always starting fresh. */
    public List<MessageResponse> getMessages(UUID userId, UUID conversationId) {
        conversationRepository.findByIdAndUserId(conversationId, userId).orElseThrow(ConversationNotFoundException::new);

        return messageRepository.findAllByConversationIdOrderByCreatedAt(conversationId).stream()
                .map(m -> new MessageResponse(m.getRole().name(), m.getContent(), m.getCreatedAt(), m.getBookIds()))
                .toList();
    }
}
