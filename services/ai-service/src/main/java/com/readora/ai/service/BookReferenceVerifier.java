package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Re-verifies, after the model has finished replying, that every book id it cited both got
 * discussed in the actual reply text and is still available at the requesting store — split out
 * of ChatService so these two independent guardrails (discussed-in-reply vs. still-available) are
 * testable without also standing up conversation persistence or the model client.
 */
@Component
public class BookReferenceVerifier {

    private static final Logger log = LoggerFactory.getLogger(BookReferenceVerifier.class);

    private final VectorStore vectorStore;
    private final CatalogClient catalogClient;

    public BookReferenceVerifier(VectorStore vectorStore, CatalogClient catalogClient) {
        this.vectorStore = vectorStore;
        this.catalogClient = catalogClient;
    }

    /**
     * A backstop against the model padding the marker with every candidate it retrieved rather
     * than only the ones it actually wrote about — observed happening in practice despite the
     * prompt's explicit cardinality rule, since no prompt instruction is bulletproof. Keeps only
     * ids whose real title actually appears in the reply text, so "the model looked at 5 books but
     * only wrote about 3" can't leave 2 unrelated cards in the response.
     */
    public List<String> filterToActuallyDiscussedIds(List<String> bookIds, String replyText) {
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
    public List<String> filterToAvailableIds(List<String> bookIds, String requestStoreId) {
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
}
