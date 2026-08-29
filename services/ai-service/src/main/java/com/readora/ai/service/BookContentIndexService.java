package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.entity.BookReaderIndex;
import com.readora.ai.entity.BookReaderIndexStatus;
import com.readora.ai.exception.BookAccessDeniedException;
import com.readora.ai.repository.BookReaderIndexRepository;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Embeds a virtual edition's PDF content for the in-reader RAG assistant, into its own pgvector
 * table — deliberately separate from the catalog-search vector_store (a different concern: whole
 * pages of book content vs. title/description/topics for product search) and, since a second
 * Spring-managed VectorStore bean of the same type would create an unqualified-injection ambiguity
 * for every existing consumer of the primary one, built and owned privately here instead of as a
 * @Bean.
 * <p>
 * A book is only ever embedded once, on whichever purchaser opens the reader first — the file is
 * identical for every owner, so re-embedding per reader would just waste embedding-API calls.
 */
@Service
public class BookContentIndexService {

    private static final Logger log = LoggerFactory.getLogger(BookContentIndexService.class);

    /** Characters per chunk — small enough for focused retrieval, large enough to keep paragraph context intact. */
    private static final int CHUNK_SIZE = 1200;
    private static final int TOP_K = 5;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final BookReaderIndexRepository indexRepository;
    private final CatalogClient catalogClient;

    private PgVectorStore bookContentVectorStore;

    public BookContentIndexService(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            BookReaderIndexRepository indexRepository,
            CatalogClient catalogClient
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.indexRepository = indexRepository;
        this.catalogClient = catalogClient;
    }

    @PostConstruct
    void init() {
        this.bookContentVectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName("ai")
                .vectorTableName("book_content_vector_store")
                .dimensions(1536)
                .initializeSchema(true)
                .build();
        this.bookContentVectorStore.afterPropertiesSet();
    }

    public BookReaderIndexStatus getStatus(UUID bookId) {
        return indexRepository.findById(bookId).map(BookReaderIndex::getStatus).orElse(null);
    }

    /**
     * Fetches the book's file, extracts its text, chunks it, and embeds every chunk — a no-op if
     * it's already ready. Ownership is checked here (not just at chat time) so indexing itself —
     * real embedding-API cost — can only ever be triggered by an actual purchaser.
     */
    @Transactional
    public BookReaderIndexStatus initialize(UUID userId, UUID bookId) {
        if (!catalogClient.isOwned(userId, bookId)) {
            throw new BookAccessDeniedException();
        }

        BookReaderIndex existing = indexRepository.findById(bookId).orElse(null);
        if (existing != null && existing.getStatus() == BookReaderIndexStatus.READY) {
            return BookReaderIndexStatus.READY;
        }

        BookReaderIndex index = existing != null ? existing : new BookReaderIndex(bookId);
        indexRepository.save(index);

        try {
            byte[] content = catalogClient.getBookContent(bookId);
            List<String> chunks = extractChunks(content);
            if (chunks.isEmpty()) {
                index.markFailed("No extractable text found in this file");
                indexRepository.save(index);
                return BookReaderIndexStatus.FAILED;
            }

            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = Map.of("bookId", bookId.toString(), "chunkIndex", i);
                // The vector table's id column is a real uuid type (same schema shape as the
                // catalog vector_store) — a composite string like "<bookId>-chunk-<i>" isn't a
                // valid UUID and fails at insert with a cryptic "UUID string too large" from
                // Postgres. Retrieval is always by the bookId *metadata* filter below, never by
                // looking a document up by its id, so the id itself just needs to be unique.
                documents.add(new Document(UUID.randomUUID().toString(), chunks.get(i), metadata));
            }
            bookContentVectorStore.add(documents);

            index.markReady(chunks.size());
            indexRepository.save(index);
            return BookReaderIndexStatus.READY;
        } catch (Exception e) {
            log.error("Failed to index book {} for the reader assistant", bookId, e);
            index.markFailed(e.getMessage());
            indexRepository.save(index);
            return BookReaderIndexStatus.FAILED;
        }
    }

    /** Top-K chunks most relevant to the question, scoped strictly to this book — never leaks another book's content. */
    public List<String> retrieveContext(UUID bookId, String question) {
        List<Document> found = bookContentVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .filterExpression("bookId == '" + bookId + "'")
                        .topK(TOP_K)
                        .build()
        );
        return found.stream().map(Document::getText).toList();
    }

    private List<String> extractChunks(byte[] pdfBytes) throws Exception {
        String text;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            text = new PDFTextStripper().getText(document);
        }

        String[] paragraphs = text.split("\\r?\\n\\s*\\r?\\n");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) continue;

            // A paragraph by itself can exceed CHUNK_SIZE (e.g. a long unbroken bullet list with
            // no blank-line breaks, common in PDFBox's extraction of dense technical PDFs) — the
            // greedy packing below only ever prevents *combining* paragraphs past the limit, so
            // an oversized one on its own must be hard-split first, or it reaches the embedding
            // model as a single document over its input-token limit and the whole run fails.
            for (String piece : splitIntoChunkSizedPieces(trimmed)) {
                if (current.length() + piece.length() > CHUNK_SIZE && current.length() > 0) {
                    chunks.add(current.toString());
                    current.setLength(0);
                }
                if (current.length() > 0) current.append("\n\n");
                current.append(piece);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }

        return chunks;
    }

    /** Splits on whitespace where possible so a piece rarely cuts a word in half; falls back to a hard cut if a single "word" is itself absurdly long. */
    private List<String> splitIntoChunkSizedPieces(String text) {
        if (text.length() <= CHUNK_SIZE) {
            return List.of(text);
        }

        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            pieces.add(text.substring(start, end).strip());
            start = end;
        }
        return pieces;
    }
}
