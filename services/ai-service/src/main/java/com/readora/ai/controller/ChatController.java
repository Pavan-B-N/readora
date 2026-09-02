package com.readora.ai.controller;

import com.readora.ai.dto.ChatRequest;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.ai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "AI Chat")
@RestController
@RequestMapping("/api/v1/ai")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(
            summary = "Send a message to the assistant",
            description = "Sends a message to the assistant and streams the reply back over text/event-stream as it's generated. The agent may call internal RAG tools and MCP tools before answering. Omit conversationId to start a new conversation.",
            tags = {"AI Chat"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token stream begins; X-Conversation-Id header carries the (possibly newly created) conversation id"),
            @ApiResponse(responseCode = "400", description = "Empty message, or message above the configured character limit"),
            @ApiResponse(responseCode = "404", description = "The conversationId does not exist or belongs to another user")
    })
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@Valid @RequestBody ChatRequest request, HttpServletResponse response) {
        ChatService.ChatStream stream = chatService.chat(CurrentUserContext.require(), request);
        response.setHeader("X-Conversation-Id", stream.conversationId().toString());
        return stream.tokens();
    }
}
