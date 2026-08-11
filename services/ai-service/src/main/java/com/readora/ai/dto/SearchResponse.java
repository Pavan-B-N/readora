package com.readora.ai.dto;

import java.util.List;

// Semantic search results for a query, ranked by similarity score.
public record SearchResponse(String query, List<Item> items) {
    // One matched book and its similarity score.
    public record Item(String bookId, String title, double score) {
    }
}
