package com.readora.ai.controller;

import com.readora.ai.dto.ConversationSummaryResponse;
import com.readora.ai.dto.MessageResponse;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.ai.service.ConversationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

    @Mock private ConversationService conversationService;

    private ConversationController controller;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ConversationController(conversationService);
        CurrentUserContext.set(userId, List.of());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void list_delegatesToServiceWithTheCurrentUserAndPageable() {
        ConversationSummaryResponse summary = new ConversationSummaryResponse(UUID.randomUUID(), "Chat", 3, Instant.now());
        Page<ConversationSummaryResponse> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1);
        when(conversationService.list(eq(userId), any())).thenReturn(page);

        var response = controller.list(0, 20);

        assertThat(response.getBody().getContent()).containsExactly(summary);
    }

    @Test
    void getMessages_delegatesToServiceWithTheCurrentUser() {
        UUID conversationId = UUID.randomUUID();
        MessageResponse message = new MessageResponse("USER", "hi", Instant.now(), List.of());
        when(conversationService.getMessages(userId, conversationId)).thenReturn(List.of(message));

        var response = controller.getMessages(conversationId);

        assertThat(response.getBody()).containsExactly(message);
    }
}
