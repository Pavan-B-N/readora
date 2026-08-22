package com.readora.commerce.client;

import com.readora.commerce.dto.BookInfo;
import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.exception.BookNotFoundException;
import com.readora.commerce.exception.InsufficientStockException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Direct service-to-service calls to catalog-service, bypassing api-gateway (as internal calls
 * do throughout this build) — sends the same shared secret the gateway stamps on requests, so
 * catalog-service's GatewaySecretFilter accepts it as a trusted internal caller.
 */
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

    public BookInfo getBook(UUID bookId) {
        return restClient.get()
                .uri("/api/v1/books/{id}", bookId)
                .retrieve()
                .onStatus(this::isNotFound, (req, res) -> {
                    throw new BookNotFoundException();
                })
                .body(BookInfo.class);
    }

    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        return restClient.post()
                .uri("/internal/inventory/reserve")
                .body(request)
                .retrieve()
                .onStatus(this::isNotFound, (req, res) -> {
                    throw new BookNotFoundException();
                })
                .onStatus(this::isConflict, (req, res) -> {
                    throw new InsufficientStockException("A title went out of stock between cart and checkout");
                })
                .body(ReserveStockResponse.class);
    }

    private boolean isNotFound(HttpStatusCode status) {
        return status.value() == 404;
    }

    private boolean isConflict(HttpStatusCode status) {
        return status.value() == 409;
    }
}
