package com.readora.mcp.client;

import com.readora.mcp.dto.BookDetail;
import com.readora.mcp.dto.BookPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public BookPage search(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/books").queryParam("q", query).build())
                .retrieve()
                .body(BookPage.class);
    }

    public BookDetail getDetail(String bookId) {
        return restClient.get()
                .uri("/api/v1/books/{id}", bookId)
                .retrieve()
                .body(BookDetail.class);
    }
}
