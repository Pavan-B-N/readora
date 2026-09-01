package com.readora.ai.tool;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadoraInternalToolsTest {

    @Mock private VectorStore vectorStore;
    @Mock private CatalogClient catalogClient;

    private ReadoraInternalTools tools;

    @BeforeEach
    void setUp() {
        tools = new ReadoraInternalTools(vectorStore, catalogClient);
    }

    private static Document doc(UUID bookId, String text) {
        return new Document(text, Map.of("bookId", bookId.toString(), "title", text));
    }

    @Test
    void ragSearchBooks_filtersToOnlyAvailableBooks() {
        UUID availableId = UUID.randomUUID();
        UUID unavailableId = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(
                List.of(doc(availableId, "Clean Code"), doc(unavailableId, "Legacy Code")));
        when(catalogClient.checkAvailability(any(), any())).thenReturn(List.of(availableId));

        List<String> results = tools.ragSearchBooks("clean code", 5, "store-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).startsWith("id: " + availableId);
    }

    @Test
    void ragSearchBooks_noCandidates_returnsEmptyWithoutCallingCatalog() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        List<String> results = tools.ragSearchBooks("nonexistent", 5, "store-1");

        assertThat(results).isEmpty();
        org.mockito.Mockito.verify(catalogClient, org.mockito.Mockito.never()).checkAvailability(any(), any());
    }

    @Test
    void ragSearchBooks_invalidStoreIdFromModel_isTreatedAsNull() {
        UUID bookId = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc(bookId, "Clean Code")));
        when(catalogClient.checkAvailability(any(), org.mockito.ArgumentMatchers.isNull())).thenReturn(List.of(bookId));

        List<String> results = tools.ragSearchBooks("clean code", 5, "not-a-real-uuid");

        assertThat(results).hasSize(1);
    }

    @Test
    void ragSearchBooks_resultLimitIsRespected() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc(id1, "A"), doc(id2, "B")));
        when(catalogClient.checkAvailability(any(), any())).thenReturn(List.of(id1, id2));

        List<String> results = tools.ragSearchBooks("query", 1, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void recommendSimilarBooks_seedNotFound_returnsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        List<String> results = tools.recommendSimilarBooks(UUID.randomUUID().toString(), 5, null);

        assertThat(results).isEmpty();
    }

    @Test
    void recommendSimilarBooks_excludesTheSeedBookItself() {
        UUID seedId = UUID.randomUUID();
        UUID neighbourId = UUID.randomUUID();
        Document seedDoc = doc(seedId, "Clean Code");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(seedDoc))
                .thenReturn(List.of(seedDoc, doc(neighbourId, "Refactoring")));
        when(catalogClient.checkAvailability(any(), any())).thenReturn(List.of(neighbourId));

        List<String> results = tools.recommendSimilarBooks(seedId.toString(), 5, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).contains(neighbourId.toString());
    }

    @Test
    void compareBooks_looksUpEachRequestedIdAndFiltersAvailability() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc(id1, "A")))
                .thenReturn(List.of(doc(id2, "B")));
        when(catalogClient.checkAvailability(any(), any())).thenReturn(List.of(id1, id2));

        List<String> results = tools.compareBooks(List.of(id1.toString(), id2.toString()), null);

        assertThat(results).hasSize(2);
    }

    @Test
    void filterAvailable_malformedBookIdInMetadata_isSkippedNotFailed() {
        Document badDoc = new Document("text", Map.of("bookId", "not-a-uuid"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(badDoc));

        List<String> results = tools.ragSearchBooks("q", 5, null);

        assertThat(results).isEmpty();
        org.mockito.Mockito.verify(catalogClient, org.mockito.Mockito.never()).checkAvailability(any(), any());
    }

    @Test
    void explainWalletBenefits_returnsStaticExplanation() {
        assertThat(tools.explainWalletBenefits()).contains("signup bonus");
    }
}
