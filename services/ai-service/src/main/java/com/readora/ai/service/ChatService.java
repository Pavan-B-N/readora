package com.readora.ai.service;

import com.readora.ai.dto.ChatRequest;
import com.readora.ai.entity.Conversation;
import com.readora.ai.entity.Message;
import com.readora.ai.entity.MessageRole;
import com.readora.ai.exception.ConversationNotFoundException;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * KNOWN GAP, flagged rather than hidden: the doc's security section requires that MCP tool calls
 * are scoped to the caller's X-User-Id from the JWT — "never from anything the model produced."
 * As wired here, mcp-server's user-scoped tools (getCart, getOrderHistory, etc.) declare userId
 * as a tool parameter, which means the model supplies it — exactly what the doc says must not
 * happen. A correct fix needs either a per-request-scoped MCP client connection carrying userId
 * as a transport-level header (not a model-visible parameter), or Spring AI's ToolContext
 * mechanism if it extends to remote MCP tools by the time you're reading this — I couldn't
 * verify either path without a live build, so this is left as real follow-up work, not silently
 * "solved."
 */
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatService(ChatClient chatClient, ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.chatClient = chatClient;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public Flux<String> chat(UUID userId, ChatRequest request) {
        Conversation conversation = resolveConversation(userId, request);

        messageRepository.save(new Message(conversation, MessageRole.USER, request.message()));

        StringBuilder assistantReply = new StringBuilder();

        return chatClient.prompt()
                .user(request.message())
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
