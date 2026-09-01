package com.readora.ai.service;

import com.readora.ai.dto.SearchResponse;
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
class SearchServiceTest {

    @Mock private VectorStore vectorStore;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(vectorStore);
    }

    @Test
    void search_mapsDocumentsToItems() {
        UUID bookId = UUID.randomUUID();
        Document doc = new Document("chunk", Map.of("bookId", bookId.toString(), "title", "Clean Code"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        SearchResponse response = searchService.search("clean code", 5);

        assertThat(response.query()).isEqualTo("clean code");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).bookId()).isEqualTo(bookId.toString());
        assertThat(response.items().get(0).title()).isEqualTo("Clean Code");
    }

    @Test
    void search_noResults_returnsEmptyItemsList() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        SearchResponse response = searchService.search("nonexistent", 5);

        assertThat(response.items()).isEmpty();
    }

    @Test
    void search_documentWithNoScore_defaultsToZero() {
        Document doc = new Document("chunk", Map.of("bookId", "b1", "title", "T"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        SearchResponse response = searchService.search("q", 5);

        assertThat(response.items().get(0).score()).isEqualTo(0.0);
    }
}
