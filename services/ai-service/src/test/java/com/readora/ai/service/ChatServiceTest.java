package com.readora.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.client.CatalogClient;
import com.readora.ai.dto.ChatRequest;
import com.readora.ai.entity.Conversation;
import com.readora.ai.entity.Message;
import com.readora.ai.exception.ConversationNotFoundException;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatClient chatClient;
    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private VectorStore vectorStore;
    @Mock private CatalogClient catalogClient;

    private ChatService chatService;
    private final UUID userId = UUID.randomUUID();

    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.StreamResponseSpec streamSpec;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                chatClient, conversationRepository, messageRepository,
                List.<ToolCallback>of(), new ToolCallback[0], new ObjectMapper(), vectorStore, catalogClient
        );

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        streamSpec = mock(ChatClient.StreamResponseSpec.class);
        org.mockito.Mockito.lenient().when(chatClient.prompt()).thenReturn(requestSpec);
        org.mockito.Mockito.lenient().when(requestSpec.stream()).thenReturn(streamSpec);
    }

    private static Conversation conversation(UUID id) {
        Conversation conversation = new Conversation(UUID.randomUUID(), "New chat");
        ReflectionTestUtils.setField(conversation, "id", id);
        return conversation;
    }

    private void streamTokens(String... tokens) {
        when(streamSpec.content()).thenReturn(Flux.just(tokens));
    }

    @Test
    void chat_noConversationId_createsANewConversation() {
        UUID newConversationId = UUID.randomUUID();
        Conversation created = conversation(newConversationId);
        when(conversationRepository.save(any())).thenReturn(created);
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(any())).thenReturn(List.of());
        streamTokens("Hello there.");

        var result = chatService.chat(userId, new ChatRequest(null, "hi", null));
        result.tokens().blockLast();

        assertThat(result.conversationId()).isEqualTo(newConversationId);
    }

    @Test
    void chat_existingConversationId_reusesIt() {
        UUID conversationId = UUID.randomUUID();
        Conversation existing = conversation(conversationId);
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(existing));
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(conversationId)).thenReturn(List.of());
        streamTokens("Continuing.");

        var result = chatService.chat(userId, new ChatRequest(conversationId.toString(), "more", null));
        result.tokens().blockLast();

        assertThat(result.conversationId()).isEqualTo(conversationId);
        org.mockito.Mockito.verify(conversationRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void chat_conversationIdNotOwnedByCaller_throws() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.chat(userId, new ChatRequest(conversationId.toString(), "hi", null)))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    /**
     * Documents real, observed behavior of stripLeadingReferenceMarker's buffering: since Flux's
     * handle() gets no final callback on completion, a short reply (under MAX_MARKER_HEADER_CHARS)
     * that never matches the marker pattern is buffered forever and never flushed to the client —
     * only the trailing bookIds frame comes through. This looks like an unintended gap (a model
     * reply that skips the REFERENCE_BOOKS marker and stays under ~400 chars would render as
     * empty), flagged separately rather than "fixed" here since this task is about coverage, not
     * behavior changes.
     */
    @Test
    void chat_shortReplyWithoutMarker_currentlyNeverFlushesReplyText() {
        Conversation created = conversation(UUID.randomUUID());
        when(conversationRepository.save(any())).thenReturn(created);
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(any())).thenReturn(List.of());
        streamTokens("Sure, ", "here's a recommendation.");

        var result = chatService.chat(userId, new ChatRequest(null, "recommend a book", null));
        List<String> frames = result.tokens().collectList().block();
        String joined = String.join("", frames);

        assertThat(joined).isEqualTo("@@RDX_BOOK_IDS@@:[]");
        org.mockito.Mockito.verify(messageRepository).save(org.mockito.Mockito.argThat(
                (Message m) -> m.getContent().isEmpty()));
    }

    @Test
    void chat_replyLongEnoughToHitTheBuffer_flushesOnceThresholdReached() {
        Conversation created = conversation(UUID.randomUUID());
        when(conversationRepository.save(any())).thenReturn(created);
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(any())).thenReturn(List.of());
        String longChunk = "x".repeat(450);
        streamTokens(longChunk);

        var result = chatService.chat(userId, new ChatRequest(null, "recommend a book", null));
        String joined = String.join("", result.tokens().collectList().block());

        assertThat(joined).contains(longChunk);
        assertThat(joined).contains("@@RDX_BOOK_IDS@@:[]");
    }

    @Test
    void chat_withReferenceMarker_extractsAndReVerifiesBookIds() {
        Conversation created = conversation(UUID.randomUUID());
        when(conversationRepository.save(any())).thenReturn(created);
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(any())).thenReturn(List.of());

        UUID bookId = UUID.randomUUID();
        String marker = "<!--REFERENCE_BOOKS:[\"" + bookId + "\"]-->\n";
        streamTokens(marker + "I recommend Clean Code.");

        Document doc = new Document("chunk", java.util.Map.of("bookId", bookId.toString(), "title", "Clean Code"));
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of(doc));
        when(catalogClient.checkAvailability(any(), any())).thenReturn(List.of(bookId));

        var result = chatService.chat(userId, new ChatRequest(null, "recommend a book", UUID.randomUUID().toString()));
        List<String> frames = result.tokens().collectList().block();
        String joined = String.join("", frames);

        assertThat(joined).contains("I recommend Clean Code.");
        assertThat(joined).contains(bookId.toString());
        assertThat(joined).doesNotContain("REFERENCE_BOOKS");
    }

    @Test
    void chat_referencedBookNotDiscussedInReply_isFilteredOut() {
        Conversation created = conversation(UUID.randomUUID());
        when(conversationRepository.save(any())).thenReturn(created);
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(any())).thenReturn(List.of());

        UUID bookId = UUID.randomUUID();
        String marker = "<!--REFERENCE_BOOKS:[\"" + bookId + "\"]-->\n";
        streamTokens(marker + "I don't have a specific recommendation right now.");

        Document doc = new Document("chunk", java.util.Map.of("bookId", bookId.toString(), "title", "Some Unrelated Title"));
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of(doc));

        var result = chatService.chat(userId, new ChatRequest(null, "recommend a book", null));
        List<String> frames = result.tokens().collectList().block();
        String joined = String.join("", frames);

        assertThat(joined).contains("@@RDX_BOOK_IDS@@:[]");
        org.mockito.Mockito.verify(catalogClient, org.mockito.Mockito.never()).checkAvailability(any(), any());
    }

    @Test
    void chat_availabilityCheckFails_dropsAllBookIdsRatherThanRiskingUnavailableOne() {
        Conversation created = conversation(UUID.randomUUID());
        when(conversationRepository.save(any())).thenReturn(created);
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(any())).thenReturn(List.of());

        UUID bookId = UUID.randomUUID();
        String marker = "<!--REFERENCE_BOOKS:[\"" + bookId + "\"]-->\n";
        streamTokens(marker + "I recommend Clean Code.");

        Document doc = new Document("chunk", java.util.Map.of("bookId", bookId.toString(), "title", "Clean Code"));
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class))).thenReturn(List.of(doc));
        when(catalogClient.checkAvailability(any(), any())).thenThrow(new RuntimeException("catalog down"));

        var result = chatService.chat(userId, new ChatRequest(null, "recommend a book", null));
        String joined = String.join("", result.tokens().collectList().block());

        assertThat(joined).contains("@@RDX_BOOK_IDS@@:[]");
    }

    @Test
    void chat_persistsPriorHistoryBeforeNewUserMessage() {
        UUID conversationId = UUID.randomUUID();
        Conversation existing = conversation(conversationId);
        Message priorUserMessage = new Message(existing, com.readora.ai.entity.MessageRole.USER, "earlier question");
        when(conversationRepository.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.of(existing));
        when(messageRepository.findAllByConversationIdOrderByCreatedAt(conversationId)).thenReturn(List.of(priorUserMessage));
        streamTokens("Following up on that.");

        chatService.chat(userId, new ChatRequest(conversationId.toString(), "follow up", null)).tokens().blockLast();

        org.mockito.Mockito.verify(requestSpec).messages(org.mockito.Mockito.argThat(
                (List<org.springframework.ai.chat.messages.Message> msgs) -> msgs.size() == 1));
    }
}
