package com.readora.ai.client;

import com.readora.ai.dto.BookDoc;
import com.readora.ai.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Direct service-to-service calls to catalog-service.
 *
 * Both methods are wrapped with a "catalog-service" circuit breaker (configured in
 * application.yml under resilience4j:). No retry here: listAllBooks is called in a loop inside a
 * Kafka consumer, and retrying inside the consumer would stall the partition — the Kafka
 * retry/DLQ mechanism handles that at a higher level instead. Resilience4j's @TimeLimiter only
 * applies to methods returning CompletableFuture, which would force this synchronous client
 * async — instead, the same configured duration bounds the underlying HTTP client's
 * connect/read timeout directly, so it stays a single config value either way.
 */
@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(
            @Value("${app.catalog-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret,
            @Value("${resilience4j.timelimiter.instances.catalog-service.timeout-duration}") Duration timeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .requestFactory(requestFactory)
                .build();
    }

    /** Pulled in for the full backfill — title, authors, description, and table of contents for every active book. */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "listAllBooksFallback")
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
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "lookupBooksFallback")
    public List<BookDoc> lookupBooks(List<UUID> bookIds) {
        BookLookupResponse response = restClient.post()
                .uri("/internal/books/lookup")
                .body(new BookLookupRequest(bookIds))
                .retrieve()
                .body(BookLookupResponse.class);

        return response != null ? response.items() : List.of();
    }

    private List<BookDoc> listAllBooksFallback(int page, int size, Throwable t) {
        throw translate(t);
    }

    private List<BookDoc> lookupBooksFallback(List<UUID> bookIds, Throwable t) {
        throw translate(t);
    }

    /**
     * Converts a circuit-breaker-open rejection into the documented "unavailable" error; any
     * other failure propagates unchanged.
     */
    private RuntimeException translate(Throwable t) {
        if (t instanceof CallNotPermittedException) {
            return new ServiceException(
                    "CATALOG_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Catalog service is currently unavailable"
            );
        }
        if (t instanceof RuntimeException re) {
            return re;
        }
        return new ServiceException(
                "CATALOG_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Catalog service is currently unavailable"
        );
    }

    private record BookExportPageResponse(List<BookDoc> items, int totalPages) {
    }

    private record BookLookupRequest(List<UUID> bookIds) {
    }

    private record BookLookupResponse(List<BookDoc> items) {
    }
}
