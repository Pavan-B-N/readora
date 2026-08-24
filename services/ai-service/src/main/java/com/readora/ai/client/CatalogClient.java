package com.readora.ai.client;

import com.readora.ai.dto.BookDoc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(
            @Value("${app.catalog-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    /** Pulled in for the full backfill — title, authors, description, and table of contents for every active book. */
    public List<BookDoc> listAllBooks(int page, int size) {
        BookExportPageResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/books/export")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(BookExportPageResponse.class);

        return response != null ? response.items() : List.of();
    }

    /** Pulled in for the incremental embedding consumer — same fields as listAllBooks, but for specific book ids only. */
    public List<BookDoc> lookupBooks(List<UUID> bookIds) {
        BookLookupResponse response = restClient.post()
                .uri("/internal/books/lookup")
                .body(new BookLookupRequest(bookIds))
                .retrieve()
                .body(BookLookupResponse.class);

        return response != null ? response.items() : List.of();
    }

    private record BookExportPageResponse(List<BookDoc> items, int totalPages) {
    }

    private record BookLookupRequest(List<UUID> bookIds) {
    }

    private record BookLookupResponse(List<BookDoc> items) {
    }
}
