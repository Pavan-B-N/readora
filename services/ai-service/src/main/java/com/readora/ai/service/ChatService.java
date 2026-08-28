package com.readora.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.client.CatalogClient;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Matches the leading "<!--REFERENCE_BOOKS:[...]-->" marker ChatClientConfig's system prompt
    // instructs the model to emit as the very first thing in every reply.
    private static final Pattern LEADING_REFERENCE_BOOKS_PATTERN =
            Pattern.compile("^\\s*<!--REFERENCE_BOOKS:(\\[[^\\]]*])-->\\s*", Pattern.DOTALL);

    // Generous enough for the marker itself (a JSON array of a handful of UUID strings) plus a
    // little slack for however the model chunks its tokens. If nothing matching the pattern has
    // appeared by then, treat it as "the model didn't include one" rather than buffering forever
    // and never showing the reply.
    private static final int MAX_MARKER_HEADER_CHARS = 400;

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final List<ToolCallback> userScopedToolCallbacks;
    private final ToolCallback[] internalToolCallbacksTemplate;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;
    private final CatalogClient catalogClient;

    public ChatService(
            ChatClient chatClient,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            List<ToolCallback> userScopedToolCallbacks,
            ToolCallback[] internalToolCallbacksTemplate,
            ObjectMapper objectMapper,
            VectorStore vectorStore,
            CatalogClient catalogClient
    ) {
        this.chatClient = chatClient;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userScopedToolCallbacks = userScopedToolCallbacks;
        this.internalToolCallbacksTemplate = internalToolCallbacksTemplate;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
        this.catalogClient = catalogClient;
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
        Flux<String> textTokens = stripLeadingReferenceMarker(rawTokens, referencedBookIds::addAll)
                .doOnNext(assistantReply::append);

        // One extra frame appended after the real tokens: persists the assistant message (its full
        // text is only known once streaming completes) and hands the same book ids — the ones the
        // model itself cited, not a fresh guess — to the client in the same stream, so a fresh
        // reply and a reloaded one both carry them the same way.
        Flux<String> tokens = textTokens.concatWith(Mono.fromCallable(() -> {
            String finalReply = assistantReply.toString();
            List<String> discussedBookIds = filterToActuallyDiscussedIds(referencedBookIds, finalReply);
            // Belt-and-suspenders on top of the tool-level guardrail: closes the gap where the model
            // recalls a book by name from earlier in the conversation (now in its history — see
            // above) without a fresh, already-filtered tool call, e.g. after the caller switched
            // stores mid-conversation. Every id shown to the user passes this check, no exceptions.
            List<String> confirmedBookIds = filterToAvailableIds(discussedBookIds, request.storeId());
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

    /**
     * Buffers just enough of the start of the model's raw output to recognize (or rule out) the
     * leading REFERENCE_BOOKS marker the system prompt asks for, hands the parsed ids to
     * bookIdsSink exactly once, and passes everything after the marker straight through unbuffered.
     * If the model never produces a well-formed marker within MAX_MARKER_HEADER_CHARS, gives up,
     * reports no book ids, and flushes whatever was buffered as ordinary reply text — a model that
     * ignores the instruction should degrade to "no carousel", not an empty or truncated reply.
     */
    private Flux<String> stripLeadingReferenceMarker(Flux<String> tokens, Consumer<List<String>> bookIdsSink) {
        StringBuilder pending = new StringBuilder();
        AtomicBoolean markerFound = new AtomicBoolean(false);
        AtomicBoolean fullyResolved = new AtomicBoolean(false);

        return tokens.handle((chunk, sink) -> {
            if (fullyResolved.get()) {
                sink.next(chunk);
                return;
            }

            pending.append(chunk);

            if (!markerFound.get()) {
                Matcher matcher = LEADING_REFERENCE_BOOKS_PATTERN.matcher(pending);
                if (matcher.find()) {
                    markerFound.set(true);
                    bookIdsSink.accept(parseBookIdsJson(matcher.group(1)));
                    pending.delete(0, matcher.end());
                } else if (pending.length() >= MAX_MARKER_HEADER_CHARS) {
                    fullyResolved.set(true);
                    bookIdsSink.accept(List.of());
                    sink.next(pending.toString());
                    return;
                } else {
                    return; // still might be an in-progress marker — keep buffering silently.
                }
            }

            // The marker pattern's trailing "\s*" only consumes whatever whitespace had already
            // arrived by the time it matched — a chunk boundary can easily land the newline right
            // after "-->" in a separate chunk from the marker itself. Keep trimming across chunks
            // until real content shows up, rather than resolving immediately and letting a lone
            // leading newline slip through into the visible reply as if it were reply text.
            String withoutLeadingWhitespace = stripLeadingWhitespace(pending.toString());
            if (!withoutLeadingWhitespace.isEmpty()) {
                fullyResolved.set(true);
                sink.next(withoutLeadingWhitespace);
                pending.setLength(0);
            }
        });
    }

    private static String stripLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(i);
    }

    /**
     * A backstop against the model padding the marker with every candidate it retrieved rather
     * than only the ones it actually wrote about — observed happening in practice despite the
     * prompt's explicit cardinality rule, since no prompt instruction is bulletproof. Keeps only
     * ids whose real title actually appears in the reply text, so "the model looked at 5 books but
     * only wrote about 3" can't leave 2 unrelated cards in the response.
     */
    private List<String> filterToActuallyDiscussedIds(List<String> bookIds, String replyText) {
        if (bookIds.isEmpty()) {
            return bookIds;
        }
        String lowerReply = replyText.toLowerCase(Locale.ROOT);
        return bookIds.stream()
                .distinct()
                .filter(id -> titleAppearsIn(id, lowerReply))
                .toList();
    }

    /**
     * The final guardrail before anything reaches the client: re-checks store availability on
     * whatever ids survived filterToActuallyDiscussedIds, independent of whether they came from a
     * tool call in this turn or the model recalling something from earlier conversation history.
     * Unlike CatalogClient.checkAvailability's own fail-closed default, an id dropped here just
     * means one fewer card — the reply text itself was already finalized and isn't retried.
     */
    private List<String> filterToAvailableIds(List<String> bookIds, String requestStoreId) {
        if (bookIds.isEmpty()) {
            return bookIds;
        }
        try {
            List<UUID> ids = bookIds.stream().map(UUID::fromString).toList();
            UUID storeId = requestStoreId == null || requestStoreId.isBlank() ? null : UUID.fromString(requestStoreId);
            Set<UUID> available = Set.copyOf(catalogClient.checkAvailability(ids, storeId));
            return bookIds.stream().filter(id -> available.contains(UUID.fromString(id))).toList();
        } catch (Exception e) {
            log.warn("Final availability check failed — dropping all book ids for this reply rather than risk showing an unavailable one", e);
            return List.of();
        }
    }

    /** Fails open (keeps the id) on a lookup error — one extra card beats silently dropping a real one. */
    private boolean titleAppearsIn(String bookId, String lowerReplyText) {
        try {
            List<Document> found = vectorStore.similaritySearch(
                    SearchRequest.builder().query("").filterExpression("bookId == '" + bookId + "'").topK(1).build()
            );
            if (found.isEmpty()) {
                return false;
            }
            Object title = found.get(0).getMetadata().get("title");
            return title != null && lowerReplyText.contains(String.valueOf(title).toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("Could not verify book id {} against the reply text — keeping it", bookId, e);
            return true;
        }
    }

    private List<String> parseBookIdsJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Model's REFERENCE_BOOKS marker wasn't valid JSON: {}", json, e);
            return List.of();
        }
    }

    private Conversation resolveConversation(UUID userId, ChatRequest request) {
        if (request.conversationId() == null) {
            return conversationRepository.save(new Conversation(userId, request.message()));
        }

        return conversationRepository.findByIdAndUserId(UUID.fromString(request.conversationId()), userId)
                .orElseThrow(ConversationNotFoundException::new);
    }
}
