package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import com.readora.ai.dto.BookDoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock private CatalogClient catalogClient;
    @Mock private VectorStore vectorStore;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(catalogClient, vectorStore);
    }

    private static BookDoc book(String id) {
        return new BookDoc(id, "Clean Code", List.of("Robert Martin"), "A guide to writing clean code.", "Ch1,Ch2");
    }

    @Test
    void backfillAll_noBooksNeedingReembedding_processesZero() {
        when(catalogClient.listBooksNeedingReembedding(50)).thenReturn(List.of());

        int total = embeddingService.backfillAll(null);

        assertThat(total).isZero();
        verify(vectorStore, never()).add(any());
    }

    @Test
    void backfillAll_singlePartialPage_embedsAndMarksThenStops() {
        BookDoc book = book(UUID.randomUUID().toString());
        when(catalogClient.listBooksNeedingReembedding(50)).thenReturn(List.of(book));

        int total = embeddingService.backfillAll(null);

        assertThat(total).isEqualTo(1);
        verify(vectorStore).add(any());
        verify(catalogClient).markEmbedded(List.of(UUID.fromString(book.id())));
    }

    @Test
    void backfillAll_fullPageThenPartialPage_loopsUntilPartial() {
        List<BookDoc> fullPage = IntStream.range(0, 50).mapToObj(i -> book(UUID.randomUUID().toString())).toList();
        List<BookDoc> partialPage = List.of(book(UUID.randomUUID().toString()));
        when(catalogClient.listBooksNeedingReembedding(50)).thenReturn(fullPage, partialPage);

        int total = embeddingService.backfillAll(null);

        assertThat(total).isEqualTo(51);
        verify(catalogClient, times(2)).listBooksNeedingReembedding(50);
    }

    @Test
    void backfillAll_invokesProgressCallbackWithRunningTotal() {
        BookDoc book = book(UUID.randomUUID().toString());
        when(catalogClient.listBooksNeedingReembedding(50)).thenReturn(List.of(book));
        java.util.List<Integer> seenTotals = new java.util.ArrayList<>();

        embeddingService.backfillAll((processed, pageBooks) -> seenTotals.add(processed));

        assertThat(seenTotals).containsExactly(1);
    }

    @Test
    void embedOne_bookFound_embedsAndMarks() {
        UUID bookId = UUID.randomUUID();
        BookDoc book = book(bookId.toString());
        when(catalogClient.lookupBooks(List.of(bookId))).thenReturn(List.of(book));

        embeddingService.embedOne(bookId);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getId()).isEqualTo(bookId.toString());
        verify(catalogClient).markEmbedded(List.of(bookId));
    }

    @Test
    void embedOne_bookNotFound_isANoOp() {
        UUID bookId = UUID.randomUUID();
        when(catalogClient.lookupBooks(List.of(bookId))).thenReturn(List.of());

        embeddingService.embedOne(bookId);

        verify(vectorStore, never()).add(any());
        verify(catalogClient, never()).markEmbedded(any());
    }

    @Test
    void embedOne_documentContentIncludesDescriptionAndTableOfContents() {
        UUID bookId = UUID.randomUUID();
        BookDoc book = book(bookId.toString());
        when(catalogClient.lookupBooks(List.of(bookId))).thenReturn(List.of(book));

        embeddingService.embedOne(bookId);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        String content = captor.getValue().get(0).getText();
        assertThat(content).contains("Clean Code by Robert Martin");
        assertThat(content).contains("A guide to writing clean code.");
        assertThat(content).contains("Covers: Ch1,Ch2");
    }

    @Test
    void embedOne_noAuthorsDescriptionOrToc_stillProducesValidContent() {
        UUID bookId = UUID.randomUUID();
        BookDoc book = new BookDoc(bookId.toString(), "Mystery Book", null, null, null);
        when(catalogClient.lookupBooks(List.of(bookId))).thenReturn(List.of(book));

        embeddingService.embedOne(bookId);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue().get(0).getText()).isEqualTo("Mystery Book by ");
    }
}
