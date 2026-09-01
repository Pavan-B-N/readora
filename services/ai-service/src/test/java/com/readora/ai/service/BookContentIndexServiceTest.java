package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.entity.BookReaderIndex;
import com.readora.ai.entity.BookReaderIndexStatus;
import com.readora.ai.exception.BookAccessDeniedException;
import com.readora.ai.repository.BookReaderIndexRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

    private PgVectorStore vectorStore;

    private BookContentIndexService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BookContentIndexService(jdbcTemplate, embeddingModel, indexRepository, catalogClient);
        // init() is a @PostConstruct that needs a real Postgres connection — the field it builds is
        // injected directly here instead, so initialize()/retrieveContext() can be tested without one.
        vectorStore = mock(PgVectorStore.class);
        ReflectionTestUtils.setField(service, "bookContentVectorStore", vectorStore);
    }

    private static byte[] pdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(25, 700);
                stream.showText(text);
                stream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
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

    @Test
    void extractChunks_realPdf_extractsTheTextIntoOneChunk() throws Exception {
        byte[] pdf = pdfWithText("Readora is a bookstore platform.");

        List<String> chunks = service.extractChunks(pdf);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("Readora is a bookstore platform.");
    }

    @Test
    void initialize_newBook_extractsChunksAndEmbedsThemThenMarksReady() throws Exception {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(indexRepository.findById(bookId)).thenReturn(Optional.empty());
        when(catalogClient.getBookContent(bookId)).thenReturn(pdfWithText("Chapter one begins here."));

        BookReaderIndexStatus status = service.initialize(userId, bookId);

        assertThat(status).isEqualTo(BookReaderIndexStatus.READY);
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat((List<Document> docs) ->
                docs.size() == 1 && docs.get(0).getMetadata().get("bookId").equals(bookId.toString())));
        verify(indexRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void initialize_pdfWithNoExtractableText_marksFailedWithoutEmbedding() throws Exception {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(indexRepository.findById(bookId)).thenReturn(Optional.empty());
        when(catalogClient.getBookContent(bookId)).thenReturn(pdfWithText(""));

        BookReaderIndexStatus status = service.initialize(userId, bookId);

        assertThat(status).isEqualTo(BookReaderIndexStatus.FAILED);
        verify(vectorStore, never()).add(any());
    }

    @Test
    void initialize_fetchingContentThrows_marksFailedRatherThanPropagating() {
        when(catalogClient.isOwned(userId, bookId)).thenReturn(true);
        when(indexRepository.findById(bookId)).thenReturn(Optional.empty());
        when(catalogClient.getBookContent(bookId)).thenThrow(new RuntimeException("catalog unreachable"));

        BookReaderIndexStatus status = service.initialize(userId, bookId);

        assertThat(status).isEqualTo(BookReaderIndexStatus.FAILED);
    }

    @Test
    void retrieveContext_scopesTheSimilaritySearchToTheGivenBook() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("relevant excerpt", java.util.Map.of("bookId", bookId.toString()))));

        List<String> context = service.retrieveContext(bookId, "what happens next?");

        assertThat(context).containsExactly("relevant excerpt");
        org.mockito.ArgumentCaptor<SearchRequest> captor = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getFilterExpression()).isNotNull();
    }
}
