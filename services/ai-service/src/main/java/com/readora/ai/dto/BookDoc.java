package com.readora.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// A catalog-service book's exported fields, as used for embedding and search.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookDoc(String id, String title, List<String> authors, String description, String tableOfContents) {
}
