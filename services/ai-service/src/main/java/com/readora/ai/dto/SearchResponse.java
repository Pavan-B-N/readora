package com.readora.ai.dto;

import java.util.List;

public record SearchResponse(String query, List<Item> items) {
    public record Item(String bookId, String title, double score) {
    }
}
