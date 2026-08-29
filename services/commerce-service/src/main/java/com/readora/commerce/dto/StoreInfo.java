package com.readora.commerce.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Mirrors catalog-service's StoreResponse, trimmed to the fields commerce-service actually needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StoreInfo(UUID id, String city) {
}
