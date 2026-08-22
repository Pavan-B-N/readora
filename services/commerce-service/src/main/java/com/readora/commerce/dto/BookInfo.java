package com.readora.commerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookInfo(UUID id, String title, String isbn13, BigDecimal listPrice, String currency, Availability availability) {
    public record Availability(String status, int quantityAvailable) {
    }
}
