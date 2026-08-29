package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.entity.BookReaderIndex;
import com.readora.ai.entity.BookReaderIndexStatus;
import com.readora.ai.exception.BookAccessDeniedException;
import com.readora.ai.repository.BookReaderIndexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the chunking policy (the exact regression class fixed earlier this project: an
 * oversized single paragraph with no blank-line breaks used to reach the embedding model as one
 * over-limit document) and the ownership/short-circuit guards in initialize() — never the actual
 * PDFBox extraction or PgVectorStore write, which both need real infrastructure {@code init()}
 * builds via @PostConstruct and are out of scope for a unit test.
 */
@ExtendWith(MockitoExtension.class)
class BookContentIndexServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private EmbeddingModel embeddingModel;
    @Mock
    private BookReaderIndexRepository indexRepository;
    @Mock
    private CatalogClient catalogClient;

    private BookContentIndexService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BookContentIndexService(jdbcTemplate, embeddingModel, indexRepository, catalogClient);
    }

    @Test
    void initialize_callerDoesNotOwnTheBook_throwsBeforeTouchingTheIndex() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(Boolean.valueOf(false));

        assertThatThrownBy(() -> service.initialize(userId, bookId))
                .isInstanceOf(BookAccessDeniedException.class);

        verify(indexRepository, never()).findById(any());
        verify(catalogClient, never()).getBookContent(any());
    }

    @Test
    void initialize_alreadyReady_shortCircuitsWithoutRefetchingTheFile() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(Boolean.valueOf(true));
        BookReaderIndex ready = new BookReaderIndex(bookId);
        ready.markReady(42);
        when(indexRepository.findById(bookId)).thenReturn(Optional.of(ready));

        BookReaderIndexStatus status = service.initialize(userId, bookId);

        assertThat(status).isEqualTo(BookReaderIndexStatus.READY);
        verify(catalogClient, never()).getBookContent(any());
        verify(indexRepository, never()).save(any());
    }

    @Test
    void splitIntoChunkSizedPieces_shortText_returnsItUnchanged() {
        List<String> pieces = service.splitIntoChunkSizedPieces("A short paragraph.");

        assertThat(pieces).containsExactly("A short paragraph.");
    }

    @Test
    void splitIntoChunkSizedPieces_oversizedParagraphWithNoBlankLines_isHardSplitOnWhitespace() {
        // Regression case: PDFBox extraction of a dense bullet list with no blank-line breaks
        // used to reach the embedding model as a single document over its input-token limit.
        String word = "readora ";
        String oversizedParagraph = word.repeat(500).strip(); // well over the 1200-char CHUNK_SIZE

        List<String> pieces = service.splitIntoChunkSizedPieces(oversizedParagraph);

        assertThat(pieces.size()).isGreaterThan(1);
        pieces.forEach(piece -> assertThat(piece.length()).isLessThanOrEqualTo(1200));
        // No word should have been cut in half by the split.
        pieces.forEach(piece -> assertThat(piece).doesNotStartWith(" ").doesNotEndWith(" "));
        assertThat(String.join(" ", pieces).replaceAll("\\s+", " "))
                .isEqualTo(oversizedParagraph.replaceAll("\\s+", " "));
    }

    @Test
    void splitIntoChunkSizedPieces_singleWordLongerThanChunkSize_hardCutsRatherThanLoopingForever() {
        String hugeUnbrokenToken = "x".repeat(2000);

        List<String> pieces = service.splitIntoChunkSizedPieces(hugeUnbrokenToken);

        assertThat(pieces.size()).isGreaterThanOrEqualTo(2);
        assertThat(String.join("", pieces)).hasSize(2000);
    }
}
