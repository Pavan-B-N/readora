package com.readora.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.dto.ChatRequest;
import com.readora.ai.entity.Conversation;
import com.readora.ai.entity.Message;
import com.readora.ai.entity.MessageRole;
import com.readora.ai.exception.ConversationNotFoundException;
import com.readora.ai.repository.ConversationRepository;
import com.readora.ai.repository.MessageRepository;
import com.readora.ai.tool.StoreScopedToolCallback;
import com.readora.ai.tool.UserScopedToolCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
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

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /**
     * A stream frame prefixed with this is metadata, not a token to display — the frontend strips
     * it out of the visible reply and parses the rest as the bookIds JSON array. Content-wise this
     * can never collide with real model output; it's not natural-language text.
     */
    private static final String BOOK_IDS_FRAME_PREFIX = "@@RDX_BOOK_IDS@@:";

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final List<ToolCallback> userScopedToolCallbacks;
    private final ToolCallback[] internalToolCallbacksTemplate;
    private final ObjectMapper objectMapper;
    private final ReferenceMarkerStreamParser referenceMarkerStreamParser;
    private final BookReferenceVerifier bookReferenceVerifier;

    public ChatService(
            ChatClient chatClient,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            List<ToolCallback> userScopedToolCallbacks,
            ToolCallback[] internalToolCallbacksTemplate,
            ObjectMapper objectMapper,
            ReferenceMarkerStreamParser referenceMarkerStreamParser,
            BookReferenceVerifier bookReferenceVerifier
    ) {
        this.chatClient = chatClient;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userScopedToolCallbacks = userScopedToolCallbacks;
        this.internalToolCallbacksTemplate = internalToolCallbacksTemplate;
        this.objectMapper = objectMapper;
        this.referenceMarkerStreamParser = referenceMarkerStreamParser;
        this.bookReferenceVerifier = bookReferenceVerifier;
    }

    /** conversationId is resolved synchronously, before streaming starts, so the controller can hand it back as a response header. */
    public record ChatStream(UUID conversationId, Flux<String> tokens) {
    }

    public ChatStream chat(UUID userId, ChatRequest request) {
        Conversation conversation = resolveConversation(userId, request);

        // Fetched before saving the new user message below, so this is exactly the prior turns —
        // the new message is added separately via .user() a few lines down, not duplicated here.
        // Without this, the model only ever sees the single latest message: a follow-up like
        // "which of these is best" would have no idea what "these" refers to, since nothing about
        // earlier turns (including a previous reply's own book ids and links) is otherwise visible
        // to it. This replaces relying on the model voluntarily calling a "load my own history"
        // tool, which it did unreliably — most concretely, a chat model should always see recent
        // history the same way any multi-turn chat API works, not as an optional lookup.
        List<org.springframework.ai.chat.messages.Message> history = messageRepository
                .findAllByConversationIdOrderByCreatedAt(conversation.getId()).stream()
                .<org.springframework.ai.chat.messages.Message>map(m -> m.getRole() == MessageRole.USER
                        ? new UserMessage(m.getContent())
                        : new AssistantMessage(m.getContent()))
                .toList();

        messageRepository.save(new Message(conversation, MessageRole.USER, request.message()));

        StringBuilder assistantReply = new StringBuilder();
        List<String> referencedBookIds = new ArrayList<>();

        List<ToolCallback> scopedCallbacks = new ArrayList<>();
        userScopedToolCallbacks.stream()
                .map(callback -> new UserScopedToolCallback(callback, userId.toString(), objectMapper))
                .forEach(scopedCallbacks::add);
        // The book-retrieval guardrail: every candidate ReadoraInternalTools hands back is already
        // filtered to what's actually purchasable at this store — see its filterAvailable() and
        // StoreScopedToolCallback's own docs for why the model's own storeId value is never trusted.
        Arrays.stream(internalToolCallbacksTemplate)
                .map(callback -> new StoreScopedToolCallback(callback, request.storeId(), objectMapper))
                .forEach(scopedCallbacks::add);

        Flux<String> rawTokens = chatClient.prompt()
                .messages(history)
                .user(request.message())
                .toolCallbacks(scopedCallbacks)
                .stream()
                .content();

        // Peels the leading marker off before any of it reaches doOnNext/the client, so the user
        // only ever sees the clean reply — never a flash of the bookkeeping comment.
        Flux<String> textTokens = referenceMarkerStreamParser.stripLeadingReferenceMarker(rawTokens, referencedBookIds::addAll)
                .doOnNext(assistantReply::append);

        // One extra frame appended after the real tokens: persists the assistant message (its full
        // text is only known once streaming completes) and hands the same book ids — the ones the
        // model itself cited, not a fresh guess — to the client in the same stream, so a fresh
        // reply and a reloaded one both carry them the same way.
        Flux<String> tokens = textTokens.concatWith(Mono.fromCallable(() -> {
            String finalReply = assistantReply.toString();
            List<String> discussedBookIds = bookReferenceVerifier.filterToActuallyDiscussedIds(referencedBookIds, finalReply);
            // Belt-and-suspenders on top of the tool-level guardrail: closes the gap where the model
            // recalls a book by name from earlier in the conversation (now in its history — see
            // above) without a fresh, already-filtered tool call, e.g. after the caller switched
            // stores mid-conversation. Every id shown to the user passes this check, no exceptions.
            List<String> confirmedBookIds = bookReferenceVerifier.filterToAvailableIds(discussedBookIds, request.storeId());
            messageRepository.save(new Message(conversation, MessageRole.ASSISTANT, finalReply, confirmedBookIds));
            try {
                return BOOK_IDS_FRAME_PREFIX + objectMapper.writeValueAsString(confirmedBookIds);
            } catch (Exception e) {
                log.warn("Failed to encode bookIds frame for conversation {}", conversation.getId(), e);
                return "";
            }
        }));

        return new ChatStream(conversation.getId(), tokens);
    }

    private Conversation resolveConversation(UUID userId, ChatRequest request) {
        if (request.conversationId() == null) {
            return conversationRepository.save(new Conversation(userId, request.message()));
        }

        return conversationRepository.findByIdAndUserId(UUID.fromString(request.conversationId()), userId)
                .orElseThrow(ConversationNotFoundException::new);
    }
}
