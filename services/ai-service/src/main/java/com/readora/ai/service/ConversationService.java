package com.readora.ai.service;

import com.readora.ai.dto.ConversationSummaryResponse;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
}
