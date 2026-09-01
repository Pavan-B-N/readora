package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.entity.BookReaderIndexStatus;
import com.readora.ai.entity.Conversation;
import com.readora.ai.entity.Message;
import com.readora.ai.entity.MessageRole;
import com.readora.ai.exception.BookAccessDeniedException;
import com.readora.ai.exception.BookNotIndexedException;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookReaderChatServiceTest {

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;
    @Mock private BookContentIndexService bookContentIndexService;
    @Mock private CatalogClient catalogClient;
    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;

    private BookReaderChatService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new BookReaderChatService(
                chatClientBuilder, bookContentIndexService, catalogClient, conversationRepository, messageRepository
        );

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        callSpec = mock(ChatClient.CallResponseSpec.class);
    }

    private static Conversation conversation(UUID id, UUID bookId) {
        Conversation conversation = new Conversation(UUID.randomUUID(), "Reader chat", bookId);
        ReflectionTestUtils.setField(conversation, "id", id);
        return conversation;
    }

    @Test
    void getHistory_callerDoesNotOwnTheBook_throws() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(userId, bookId)).isInstanceOf(BookAccessDeniedException.class);
    }

    @Test
    void getHistory_noConversationYet_returnsEmpty() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(conversationRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());

        assertThat(service.getHistory(userId, bookId)).isEmpty();
    }

    @Test
    void getHistory_existingConversation_mapsMessagesToReaderMessages() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        Conversation conv = conversation(UUID.randomUUID(), bookId);
        when(conversationRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(conv));
        Message msg = new Message(conv, MessageRole.USER, "what happens next?");
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(conv.getId())).thenReturn(List.of(msg));

        List<BookReaderChatService.ReaderMessage> history = service.getHistory(userId, bookId);

        assertThat(history).containsExactly(new BookReaderChatService.ReaderMessage("USER", "what happens next?"));
    }

    @Test
    void chat_callerDoesNotOwnTheBook_throwsBeforeCheckingIndexStatus() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(false);

        assertThatThrownBy(() -> service.chat(userId, bookId, "hi")).isInstanceOf(BookAccessDeniedException.class);

        verify(bookContentIndexService, never()).getStatus(any());
    }

    @Test
    void chat_bookNotIndexed_throws() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(bookContentIndexService.getStatus(bookId)).thenReturn(BookReaderIndexStatus.FAILED);

        assertThatThrownBy(() -> service.chat(userId, bookId, "hi")).isInstanceOf(BookNotIndexedException.class);

        verify(conversationRepository, never()).findByUserIdAndBookId(any(), any());
    }

    @Test
    void chat_newConversation_createsItAndPersistsBothMessages() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(bookContentIndexService.getStatus(bookId)).thenReturn(BookReaderIndexStatus.READY);
        when(conversationRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());
        Conversation created = conversation(UUID.randomUUID(), bookId);
        when(conversationRepository.save(any())).thenReturn(created);
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(created.getId())).thenReturn(List.of());
        when(bookContentIndexService.retrieveContext(bookId, "what happens next?"))
                .thenReturn(List.of("Chapter 1 excerpt"));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("The hero wins.");

        String reply = service.chat(userId, bookId, "what happens next?");

        assertThat(reply).isEqualTo("The hero wins.");
        verify(messageRepository).save(argThatUserMessage("what happens next?"));
        verify(messageRepository).save(argThatAssistantMessage("The hero wins."));
    }

    @Test
    void chat_existingConversation_reusesItAndIncludesPriorHistory() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(bookContentIndexService.getStatus(bookId)).thenReturn(BookReaderIndexStatus.READY);
        Conversation existing = conversation(UUID.randomUUID(), bookId);
        when(conversationRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(existing));
        Message priorUser = new Message(existing, MessageRole.USER, "who is the protagonist?");
        Message priorAssistant = new Message(existing, MessageRole.ASSISTANT, "Alice.");
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(existing.getId()))
                .thenReturn(List.of(priorUser, priorAssistant));
        when(bookContentIndexService.retrieveContext(bookId, "what happens next?")).thenReturn(List.of());
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("She wins.");

        String reply = service.chat(userId, bookId, "what happens next?");

        assertThat(reply).isEqualTo("She wins.");
        verify(conversationRepository, never()).save(any());
        verify(requestSpec).messages(List.<org.springframework.ai.chat.messages.Message>of(
                new org.springframework.ai.chat.messages.UserMessage("who is the protagonist?"),
                new org.springframework.ai.chat.messages.AssistantMessage("Alice.")));
    }

    @Test
    void chat_nullReplyFromModel_savesAndReturnsAFallbackMessage() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(bookContentIndexService.getStatus(bookId)).thenReturn(BookReaderIndexStatus.READY);
        Conversation existing = conversation(UUID.randomUUID(), bookId);
        when(conversationRepository.findByUserIdAndBookId(userId, bookId)).thenReturn(Optional.of(existing));
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(existing.getId())).thenReturn(List.of());
        when(bookContentIndexService.retrieveContext(bookId, "hi")).thenReturn(List.of());
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(null);

        String reply = service.chat(userId, bookId, "hi");

        assertThat(reply).isEqualTo("I couldn't come up with an answer just now — try asking again.");
        verify(messageRepository).save(argThatAssistantMessage(reply));
    }

    private static Message argThatUserMessage(String content) {
        return org.mockito.ArgumentMatchers.argThat(m -> m.getRole() == MessageRole.USER && m.getContent().equals(content));
    }

    private static Message argThatAssistantMessage(String content) {
        return org.mockito.ArgumentMatchers.argThat(m -> m.getRole() == MessageRole.ASSISTANT && m.getContent().equals(content));
    }
}
