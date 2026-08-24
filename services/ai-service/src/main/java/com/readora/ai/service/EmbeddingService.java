package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.dto.BookDoc;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    /** Re-embeds every book in the catalog, paginating through catalog-service's full export. */
    public void backfillAll() {
        int page = 0;
        List<BookDoc> books;

        do {
            books = catalogClient.listAllBooks(page, PAGE_SIZE);
            if (!books.isEmpty()) {
                embed(books);
            }
            page++;
        } while (books.size() == PAGE_SIZE);
    }

    /** Re-embeds a single book, replacing its existing vector if one exists. */
    public void embedOne(UUID bookId) {
        List<BookDoc> books = catalogClient.lookupBooks(List.of(bookId));
        if (!books.isEmpty()) {
            embed(books);
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
