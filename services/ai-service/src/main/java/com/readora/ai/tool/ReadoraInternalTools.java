package com.readora.ai.tool;

import com.readora.ai.repository.MessageRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * In-process tools — operate on this service's own vector store and Postgres, no network hop.
 * Unverified against a live build (VectorStore/SearchRequest API surface) — see build notes.
 */
@Component
public class ReadoraInternalTools {

    private final VectorStore vectorStore;
    private final MessageRepository messageRepository;

    public ReadoraInternalTools(VectorStore vectorStore, MessageRepository messageRepository) {
        this.vectorStore = vectorStore;
        this.messageRepository = messageRepository;
    }

    @Tool(description = "Semantic search over the book catalogue — returns the nearest books to a free-text query")
    public List<String> ragSearchBooks(
            @ToolParam(description = "search query") String query,
            @ToolParam(description = "max results") int limit
    ) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(limit).build()
        );
        return results.stream().map(Document::getText).toList();
    }

    @Tool(description = "Find books similar to a given book id — vector neighbours, not keyword overlap")
    public List<String> recommendSimilarBooks(
            @ToolParam(description = "the book id to find neighbours of") String bookId,
            @ToolParam(description = "max results") int limit
    ) {
        List<Document> seed = vectorStore.similaritySearch(
                SearchRequest.builder().query("").filterExpression("bookId == '" + bookId + "'").topK(1).build()
        );
        if (seed.isEmpty()) {
            return List.of();
        }

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(seed.get(0).getText()).topK(limit + 1).build()
        );

        return results.stream()
                .filter(d -> !bookId.equals(d.getMetadata().get("bookId")))
                .limit(limit)
                .map(Document::getText)
                .toList();
    }

    @Tool(description = "Gather full text for specific book ids already retrieved into context, for the model to compose a comparison from")
    public List<String> compareBooks(@ToolParam(description = "book ids to compare") List<String> bookIds) {
        return bookIds.stream()
                .flatMap(id -> vectorStore.similaritySearch(
                        SearchRequest.builder().query("").filterExpression("bookId == '" + id + "'").topK(1).build()
                ).stream())
                .map(Document::getText)
                .toList();
    }

    @Tool(description = "Explain Readora's signup bonus, referral bonus and wallet-pay discount rules")
    public String explainWalletBenefits() {
        return "New accounts receive a 100.00 signup bonus. A successful referral credits 200.00 "
                + "to the referrer. Paying (partly) from wallet balance applies a 10% discount on "
                + "the wallet-funded portion of an order.";
    }

    @Tool(description = "Load prior turns for a conversation so a follow-up question keeps its context")
    public List<String> conversationMemory(@ToolParam(description = "the conversation id") String conversationId) {
        return messageRepository.findAllByConversationIdOrderByCreatedAt(UUID.fromString(conversationId)).stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .toList();
    }
}
