package com.readora.commerce.dto;

import java.util.List;

/** Mirrors catalog-service's response of the same shape. */
public record BookCoverLookupResponse(List<BookCover> items) {
}
