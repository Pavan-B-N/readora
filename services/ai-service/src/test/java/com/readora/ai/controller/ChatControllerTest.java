package com.readora.ai.controller;

import com.readora.ai.dto.ChatRequest;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.ai.service.ChatService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock private ChatService chatService;
    @Mock private HttpServletResponse httpResponse;

    private ChatController controller;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService);
        CurrentUserContext.set(userId, List.of());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void chat_setsConversationIdHeaderAndReturnsTheTokenStream() {
        UUID conversationId = UUID.randomUUID();
        ChatService.ChatStream stream = new ChatService.ChatStream(conversationId, Flux.just("hello", " world"));
        ChatRequest request = new ChatRequest(null, "hi", null);
        when(chatService.chat(userId, request)).thenReturn(stream);

        Flux<String> result = controller.chat(request, httpResponse);

        assertThat(result.collectList().block()).containsExactly("hello", " world");
        verify(httpResponse).setHeader("X-Conversation-Id", conversationId.toString());
    }
}
