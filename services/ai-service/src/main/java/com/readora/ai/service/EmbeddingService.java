package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.dto.BookDoc;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Builds and stores book embeddings in the vector store — the single place that logic lives,
 * used by both the admin-triggered full backfill and the incremental Kafka consumer. Uses each
 * book's id as the document id, so re-embedding a book overwrites its existing vector rather
 * than duplicating it.
 */
@Service
public class EmbeddingService {

    private static final int PAGE_SIZE = 50;

    private final CatalogClient catalogClient;
    private final VectorStore vectorStore;

    public EmbeddingService(CatalogClient catalogClient, VectorStore vectorStore) {
        this.catalogClient = catalogClient;
        this.vectorStore = vectorStore;
    }

    /**
     * Re-embeds every book that's never been embedded or has changed since its last embedding —
     * skips books whose embedding is already current, so a repeat backfill run is cheap.
     * <p>
     * Always re-queries page 0: catalog-service marks each returned batch embedded right after
     * it's processed here, which drops those books out of the "needs reembedding" set, so the
     * next page-0 fetch naturally returns whatever wasn't covered yet. Walking page 1, 2, 3... on
     * a set that shrinks under you would skip books.
     *
     * @param progressCallback invoked after each page with (booksProcessedSoFar, thisPageBooks) —
     *                         the full page, not just the last title, so a caller can log every
     *                         book that was just embedded rather than only the most recent one.
     *                         Pass null to skip reporting.
     * @return the total number of books embedded
     */
    public int backfillAll(BiConsumer<Integer, List<BookDoc>> progressCallback) {
        int processed = 0;
        List<BookDoc> books;

        do {
            books = catalogClient.listBooksNeedingReembedding(PAGE_SIZE);
            if (!books.isEmpty()) {
                embed(books);
                catalogClient.markEmbedded(books.stream().map(b -> UUID.fromString(b.id())).toList());
                processed += books.size();
                if (progressCallback != null) {
                    progressCallback.accept(processed, books);
                }
            }
        } while (books.size() == PAGE_SIZE);

        return processed;
    }

    /** Re-embeds a single book, replacing its existing vector if one exists. */
    public void embedOne(UUID bookId) {
        List<BookDoc> books = catalogClient.lookupBooks(List.of(bookId));
        if (!books.isEmpty()) {
            embed(books);
            catalogClient.markEmbedded(List.of(bookId));
        }
    }

    private void embed(List<BookDoc> books) {
        vectorStore.add(books.stream().map(this::toDocument).toList());
    }

    private Document toDocument(BookDoc book) {
        String authors = book.authors() != null ? String.join(", ", book.authors()) : "";

        StringBuilder content = new StringBuilder(book.title()).append(" by ").append(authors);

        if (book.description() != null && !book.description().isBlank()) {
            content.append(". ").append(book.description());
        }
        if (book.tableOfContents() != null && !book.tableOfContents().isBlank()) {
            content.append(". Covers: ").append(book.tableOfContents());
        }

        Map<String, Object> metadata = Map.of(
                "bookId", book.id(),
                "title", book.title()
        );

        return new Document(book.id(), content.toString(), metadata);
    }
}
