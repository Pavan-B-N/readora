package com.readora.ai.service;

import com.readora.ai.client.CatalogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookReferenceVerifierTest {

    @Mock private VectorStore vectorStore;
    @Mock private CatalogClient catalogClient;

    private BookReferenceVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new BookReferenceVerifier(vectorStore, catalogClient);
    }

    @Test
    void filterToActuallyDiscussedIds_emptyInput_returnsEmpty() {
        assertThat(verifier.filterToActuallyDiscussedIds(List.of(), "some reply")).isEmpty();
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void filterToActuallyDiscussedIds_titleAppearsInReply_isKept() {
        UUID bookId = UUID.randomUUID();
        Document doc = new Document("chunk", Map.of("bookId", bookId.toString(), "title", "Clean Code"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        List<String> result = verifier.filterToActuallyDiscussedIds(
                List.of(bookId.toString()), "I recommend Clean Code.");

        assertThat(result).containsExactly(bookId.toString());
    }

    @Test
    void filterToActuallyDiscussedIds_titleNotInReply_isFilteredOut() {
        UUID bookId = UUID.randomUUID();
        Document doc = new Document("chunk", Map.of("bookId", bookId.toString(), "title", "Some Unrelated Title"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        List<String> result = verifier.filterToActuallyDiscussedIds(
                List.of(bookId.toString()), "I don't have a specific recommendation right now.");

        assertThat(result).isEmpty();
    }

    @Test
    void filterToActuallyDiscussedIds_noLongerInVectorStore_isFilteredOutWithoutThrowing() {
        UUID bookId = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        List<String> result = verifier.filterToActuallyDiscussedIds(
                List.of(bookId.toString()), "I recommend Clean Code.");

        assertThat(result).isEmpty();
    }

    @Test
    void filterToActuallyDiscussedIds_lookupThrows_failsOpenAndKeepsId() {
        UUID bookId = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("vector store down"));

        List<String> result = verifier.filterToActuallyDiscussedIds(
                List.of(bookId.toString()), "I recommend Clean Code.");

        assertThat(result).containsExactly(bookId.toString());
    }

    @Test
    void filterToAvailableIds_emptyInput_returnsEmpty() {
        assertThat(verifier.filterToAvailableIds(List.of(), "store-1")).isEmpty();
        verify(catalogClient, never()).checkAvailability(any(), any());
    }

    @Test
    void filterToAvailableIds_available_isKept() {
        UUID bookId = UUID.randomUUID();
        when(catalogClient.checkAvailability(any(), any())).thenReturn(List.of(bookId));

        List<String> result = verifier.filterToAvailableIds(List.of(bookId.toString()), UUID.randomUUID().toString());

        assertThat(result).containsExactly(bookId.toString());
    }

    @Test
    void filterToAvailableIds_notAvailable_isFilteredOut() {
        UUID bookId = UUID.randomUUID();
        when(catalogClient.checkAvailability(any(), any())).thenReturn(List.of());

        List<String> result = verifier.filterToAvailableIds(List.of(bookId.toString()), null);

        assertThat(result).isEmpty();
    }

    @Test
    void filterToAvailableIds_checkThrows_dropsAllIdsRatherThanRiskingUnavailableOne() {
        UUID bookId = UUID.randomUUID();
        when(catalogClient.checkAvailability(any(), any())).thenThrow(new RuntimeException("catalog down"));

        List<String> result = verifier.filterToAvailableIds(List.of(bookId.toString()), null);

        assertThat(result).isEmpty();
    }
}
