package com.readora.ai.tool;

import com.readora.ai.client.CatalogClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// In-process tools operating on this service's own vector store (plus one catalog-service call per invocation to enforce store availability); every book-retrieval tool takes a storeId parameter but the model's own value is never trusted — StoreScopedToolCallback overwrites it with the caller's real selected store before the call goes out (same mechanism UserScopedToolCallback uses for userId), so even a model that ignores instructions or invents a store id can't get an unavailable book past filterAvailable() below since the parameter it's overridden with is always genuine.
@Component
public class ReadoraInternalTools {

    // Over-fetches by this factor before filtering, since some raw candidates will turn out unavailable at the caller's store and get dropped, so without slack an available-only result could come back short of the requested `limit` even when the catalog has enough good matches.
    private static final int CANDIDATE_OVERFETCH_FACTOR = 4;

    private final VectorStore vectorStore;
    private final CatalogClient catalogClient;

    // Wires the vector store and catalog client used by the tools below.
    public ReadoraInternalTools(VectorStore vectorStore, CatalogClient catalogClient) {
        this.vectorStore = vectorStore;
        this.catalogClient = catalogClient;
    }

    // Vector-searches the catalogue by free text and returns only currently available matches.
    @Tool(description = "Semantic search over the book catalogue — returns the nearest books to a free-text query, "
            + "restricted to books actually available (in stock at the caller's store, or virtual) right now. "
            + "Each result is prefixed with its real book id as 'id: <uuid> | <details>' — cite that id verbatim "
            + "(via the REFERENCE_BOOKS marker and any inline link) when you recommend the book; never invent one.")
    public List<String> ragSearchBooks(
            @ToolParam(description = "search query") String query,
            @ToolParam(description = "max results") int limit,
            @ToolParam(description = "the caller's store id — filled in automatically; omit it or pass anything, it is ignored", required = false) String storeId
    ) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(limit * CANDIDATE_OVERFETCH_FACTOR).build()
        );
        return filterAvailable(results, storeId, limit);
    }

    // Finds vector neighbours of a given book id and returns only currently available matches.
    @Tool(description = "Find books similar to a given book id — vector neighbours, not keyword overlap — restricted "
            + "to books actually available (in stock at the caller's store, or virtual) right now. Each result is "
            + "prefixed with its real book id as 'id: <uuid> | <details>' — cite that id verbatim (via the "
            + "REFERENCE_BOOKS marker and any inline link) when you recommend the book; never invent one.")
    public List<String> recommendSimilarBooks(
            @ToolParam(description = "the book id to find neighbours of") String bookId,
            @ToolParam(description = "max results") int limit,
            @ToolParam(description = "the caller's store id — filled in automatically; omit it or pass anything, it is ignored", required = false) String storeId
    ) {
        List<Document> seed = vectorStore.similaritySearch(
                SearchRequest.builder().query("").filterExpression("bookId == '" + bookId + "'").topK(1).build()
        );
        if (seed.isEmpty()) {
            return List.of();
        }

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(seed.get(0).getText()).topK(limit * CANDIDATE_OVERFETCH_FACTOR + 1).build()
        );

        List<Document> neighbours = results.stream()
                .filter(d -> !bookId.equals(String.valueOf(d.getMetadata().get("bookId"))))
                .toList();

        return filterAvailable(neighbours, storeId, limit);
    }

    // Looks up full text for specific book ids and returns only ones still currently available.
    @Tool(description = "Gather full text for specific book ids already retrieved into context, for the model to "
            + "compose a comparison from — restricted to ones still actually available (in stock at the caller's "
            + "store, or virtual) right now. Each result is prefixed with its real book id as 'id: <uuid> | <details>' "
            + "— cite that id verbatim (via the REFERENCE_BOOKS marker and any inline link); never invent one.")
    public List<String> compareBooks(
            @ToolParam(description = "book ids to compare") List<String> bookIds,
            @ToolParam(description = "the caller's store id — filled in automatically; omit it or pass anything, it is ignored", required = false) String storeId
    ) {
        List<Document> found = bookIds.stream()
                .flatMap(id -> vectorStore.similaritySearch(
                        SearchRequest.builder().query("").filterExpression("bookId == '" + id + "'").topK(1).build()
                ).stream())
                .toList();

        return filterAvailable(found, storeId, found.size());
    }

    // The actual enforcement point: resolves each document's real book id, asks catalog-service which are purchasable at storeId, and keeps only those in the vector store's original relevance order (capped at limit); a catalog-service outage fails closed (see CatalogClient.checkAvailability) rather than letting every candidate through unchecked.
    private List<String> filterAvailable(List<Document> documents, String storeId, int limit) {
        if (documents.isEmpty()) {
            return List.of();
        }

        Map<UUID, Document> byId = new LinkedHashMap<>();
        for (Document document : documents) {
            Object rawId = document.getMetadata().get("bookId");
            if (rawId == null) {
                continue;
            }
            try {
                byId.putIfAbsent(UUID.fromString(String.valueOf(rawId)), document);
            } catch (IllegalArgumentException ignored) {
                // A malformed bookId in the vector store's metadata — skip it rather than fail the whole search.
            }
        }
        if (byId.isEmpty()) {
            return List.of();
        }

        Set<UUID> availableIds = Set.copyOf(catalogClient.checkAvailability(List.copyOf(byId.keySet()), parseStoreId(storeId)));

        return byId.entrySet().stream()
                .filter(entry -> availableIds.contains(entry.getKey()))
                .limit(limit)
                .map(entry -> withBookId(entry.getKey(), entry.getValue()))
                .toList();
    }

    // Parses a store id string, returning null if blank or malformed.
    private static UUID parseStoreId(String storeId) {
        try {
            return storeId == null || storeId.isBlank() ? null : UUID.fromString(storeId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Prefixes a document's text with its real book id.
    private static String withBookId(UUID bookId, Document document) {
        return "id: " + bookId + " | " + document.getText();
    }

    // Returns Readora's static wallet-benefit rules text.
    @Tool(description = "Explain Readora's signup bonus, referral bonus and wallet-pay discount rules")
    public String explainWalletBenefits() {
        return "New accounts receive a 100.00 signup bonus. A successful referral credits 200.00 "
                + "to the referrer. Paying (partly) from wallet balance applies a 10% discount on "
                + "the wallet-funded portion of an order.";
    }
}
