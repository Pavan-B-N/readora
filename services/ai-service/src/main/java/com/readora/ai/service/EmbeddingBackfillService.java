package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.dto.BookDoc;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Backfills the vector store from catalog-service on every startup, using each book's id as the
 * document id so re-running overwrites rather than duplicates. This is a simplification of the
 * doc's "refreshed on text-field update" behavior (which implies event-driven incremental
 * updates) — there's no catalog.book.updated event in this system yet, so a full re-embed on
 * startup is the pragmatic stand-in.
 */
@Component
public class EmbeddingBackfillService implements ApplicationRunner {

    private static final int PAGE_SIZE = 50;

    private final CatalogClient catalogClient;
    private final VectorStore vectorStore;

    public EmbeddingBackfillService(CatalogClient catalogClient, VectorStore vectorStore) {
        this.catalogClient = catalogClient;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        int page = 0;
        List<BookDoc> books;

        do {
            books = catalogClient.listAllBooks(page, PAGE_SIZE);
            if (!books.isEmpty()) {
                vectorStore.add(books.stream().map(this::toDocument).toList());
            }
            page++;
        } while (books.size() == PAGE_SIZE);
    }

    private Document toDocument(BookDoc book) {
        String authors = book.authors() != null ? String.join(", ", book.authors()) : "";
        String content = book.title() + " by " + authors;

        Map<String, Object> metadata = Map.of(
                "bookId", book.id(),
                "title", book.title()
        );

        return new Document(book.id(), content, metadata);
    }
}
