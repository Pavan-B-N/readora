package com.readora.ai.service;

import com.readora.ai.dto.SearchResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/** Pure retrieval, no LLM call — GET /ai/search sits behind the normal search box. */
@Service
public class SearchService {

    private final VectorStore vectorStore;

    public SearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public SearchResponse search(String query, int limit) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(limit).build()
        );

        List<SearchResponse.Item> items = results.stream()
                .map(doc -> new SearchResponse.Item(
                        String.valueOf(doc.getMetadata().get("bookId")),
                        String.valueOf(doc.getMetadata().get("title")),
                        doc.getScore() != null ? doc.getScore() : 0.0
                ))
                .toList();

        return new SearchResponse(query, items);
    }
}
