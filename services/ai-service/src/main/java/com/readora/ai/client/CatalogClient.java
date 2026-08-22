package com.readora.ai.client;

import com.readora.ai.dto.BookDoc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

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

    /** Pulled in for the embedding backfill — every active book, one page at a time. */
    public List<BookDoc> listAllBooks(int page, int size) {
        BookPageResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/books")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(BookPageResponse.class);

        return response != null ? response.items() : List.of();
    }

    private record BookPageResponse(List<BookDoc> items, int totalPages) {
    }
}
