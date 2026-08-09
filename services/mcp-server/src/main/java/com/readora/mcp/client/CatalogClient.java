package com.readora.mcp.client;

import com.readora.mcp.dto.BookDetail;
import com.readora.mcp.dto.BookPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Talks to catalog-service on behalf of MCP tools, authenticating requests with the shared gateway secret.
@Component
public class CatalogClient {

    private final RestClient restClient;

    // Builds the REST client, pre-authenticated with the shared gateway secret.
    public CatalogClient(
            @Value("${app.catalog-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    // Searches the catalog for books matching the query.
    public BookPage search(String query) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/books").queryParam("q", query).build())
                .retrieve()
                .body(BookPage.class);
    }

    // Fetches full details for a single book.
    public BookDetail getDetail(String bookId) {
        return restClient.get()
                .uri("/api/v1/books/{id}", bookId)
                .retrieve()
                .body(BookDetail.class);
    }
}
