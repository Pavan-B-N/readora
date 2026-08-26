package com.readora.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.dto.ChatRequest;
import com.readora.ai.entity.Conversation;
import com.readora.ai.entity.Message;
import com.readora.ai.entity.MessageRole;
import com.readora.ai.exception.ConversationNotFoundException;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import com.readora.ai.tool.UserScopedToolCallback;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * The MCP tools that read a specific user's own data (getCart, getOrderHistory, etc.) declare
 * userId as a parameter, but the model must never be trusted to supply it — see
 * ChatClientConfig's userScopedToolCallbacks() bean. Each chat request wraps those callbacks with
 * the caller's real, JWT-authenticated user id (never from request.message() or anything the
 * model produced) before making them available for this one call.
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final List<ToolCallback> userScopedToolCallbacks;
    private final ObjectMapper objectMapper;

    public ChatService(
            ChatClient chatClient,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            List<ToolCallback> userScopedToolCallbacks,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClient;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userScopedToolCallbacks = userScopedToolCallbacks;
        this.objectMapper = objectMapper;
    }

    public Flux<String> chat(UUID userId, ChatRequest request) {
        Conversation conversation = resolveConversation(userId, request);

        messageRepository.save(new Message(conversation, MessageRole.USER, request.message()));

        StringBuilder assistantReply = new StringBuilder();

        List<ToolCallback> scopedCallbacks = userScopedToolCallbacks.stream()
                .map(callback -> new UserScopedToolCallback(callback, userId.toString(), objectMapper))
                .map(ToolCallback.class::cast)
                .toList();

        return chatClient.prompt()
                .user(request.message())
                .toolCallbacks(scopedCallbacks)
                .stream()
                .content()
                .doOnNext(assistantReply::append)
                .doOnComplete(() ->
                        messageRepository.save(new Message(conversation, MessageRole.ASSISTANT, assistantReply.toString()))
                );
    }

    private Conversation resolveConversation(UUID userId, ChatRequest request) {
        if (request.conversationId() == null) {
            return conversationRepository.save(new Conversation(userId, request.message()));
        }

        return conversationRepository.findByIdAndUserId(UUID.fromString(request.conversationId()), userId)
                .orElseThrow(ConversationNotFoundException::new);
    }
}
