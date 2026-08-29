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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The reader's AI assistant — deliberately a plain retrieval-augmented Q&A over one book's own
 * content, not the general shopping assistant (ChatService): no tool calling, no book
 * recommendations, no access to other books. A focused reading companion that only ever answers
 * from what's actually in the book the reader has open.
 */
@Service
public class BookReaderChatService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a reading companion for the book the user currently has open. Answer questions \
            using ONLY the excerpts from the book provided below — never your own outside knowledge, \
            and never information about any other book. If the excerpts don't contain the answer, \
            say plainly that the book doesn't seem to cover that, rather than guessing.

            Relevant excerpts from the book:
            %s
            """;

    private final ChatClient chatClient;
    private final BookContentIndexService bookContentIndexService;
    private final CatalogClient catalogClient;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public BookReaderChatService(
            ChatClient.Builder chatClientBuilder,
            BookContentIndexService bookContentIndexService,
            CatalogClient catalogClient,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        // Built once from a fresh builder chain here, not the shared `chatClient` bean — this
        // assistant must never inherit the shopping assistant's tools or system prompt.
        this.chatClient = chatClientBuilder.build();
        this.bookContentIndexService = bookContentIndexService;
        this.catalogClient = catalogClient;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public record ReaderMessage(String role, String content) {
    }

    @Transactional(readOnly = true)
    public List<ReaderMessage> getHistory(UUID userId, UUID bookId) {
        requireOwned(userId, bookId);
        return conversationRepository.findByUserIdAndBookId(userId, bookId)
                .map(c -> messageRepository.findAllByConversationIdOrderByCreatedAt(c.getId()).stream()
                        .map(m -> new ReaderMessage(m.getRole().name(), m.getContent()))
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public String chat(UUID userId, UUID bookId, String question) {
        requireOwned(userId, bookId);
        if (bookContentIndexService.getStatus(bookId) != BookReaderIndexStatus.READY) {
            throw new BookNotIndexedException();
        }

        Conversation conversation = conversationRepository.findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> conversationRepository.save(new Conversation(userId, "Reader chat", bookId)));

        List<org.springframework.ai.chat.messages.Message> history = messageRepository
                .findAllByConversationIdOrderByCreatedAt(conversation.getId()).stream()
                .<org.springframework.ai.chat.messages.Message>map(m -> m.getRole() == MessageRole.USER
                        ? new UserMessage(m.getContent())
                        : new AssistantMessage(m.getContent()))
                .toList();

        messageRepository.save(new Message(conversation, MessageRole.USER, question));

        List<String> context = bookContentIndexService.retrieveContext(bookId, question);
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(String.join("\n---\n", context));

        String reply = chatClient.prompt()
                .system(systemPrompt)
                .messages(history)
                .user(question)
                .call()
                .content();

        String finalReply = reply != null ? reply : "I couldn't come up with an answer just now — try asking again.";
        messageRepository.save(new Message(conversation, MessageRole.ASSISTANT, finalReply));
        return finalReply;
    }

    private void requireOwned(UUID userId, UUID bookId) {
        if (!catalogClient.isOwned(userId, bookId)) {
            throw new BookAccessDeniedException();
        }
    }
}
