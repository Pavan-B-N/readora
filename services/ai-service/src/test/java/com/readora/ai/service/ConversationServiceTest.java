package com.readora.ai.service;

import com.readora.ai.entity.Conversation;
import com.readora.ai.entity.Message;
import com.readora.ai.entity.MessageRole;
import com.readora.ai.exception.ConversationNotFoundException;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;

    private ConversationService conversationService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        conversationService = new ConversationService(conversationRepository, messageRepository);
    }

    private static Conversation conversation(UUID id) {
        Conversation conversation = new Conversation(UUID.randomUUID(), "New chat");
        ReflectionTestUtils.setField(conversation, "id", id);
        return conversation;
    }

    @Test
    void list_mapsConversationsWithMessageCounts() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = conversation(conversationId);
        when(conversationRepository.findAllByUserIdOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(new PageImpl<>(List.of(conversation)));
        when(messageRepository.countByConversationId(conversationId)).thenReturn(3L);

        var page = conversationService.list(userId, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).messageCount()).isEqualTo(3L);
    }

    @Test
    void getMessages_notOwnedByCaller_throws() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getMessages(userId, conversationId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void getMessages_found_mapsMessagesInOrder() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = conversation(conversationId);
        Message message = new Message(conversation, MessageRole.USER, "hello");
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(conversationId)).thenReturn(List.of(message));

        var messages = conversationService.getMessages(userId, conversationId);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).content()).isEqualTo("hello");
        assertThat(messages.get(0).role()).isEqualTo("USER");
    }
}
