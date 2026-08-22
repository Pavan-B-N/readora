package com.readora.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookDoc(String id, String title, List<String> authors, BigDecimal listPrice) {
}
