package com.readora.commerce.client;

import com.readora.commerce.dto.BookCover;
import com.readora.commerce.dto.BookCoverLookupRequest;
import com.readora.commerce.dto.BookCoverLookupResponse;
import com.readora.commerce.dto.BookInfo;
import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.ReserveStockResponse;
import com.readora.commerce.dto.VirtualEditionLookupRequest;
import com.readora.commerce.dto.VirtualEditionLookupResponse;
import com.readora.commerce.exception.BookNotFoundException;
import com.readora.commerce.exception.InsufficientStockException;
import com.readora.commerce.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Direct service-to-service calls to catalog-service, bypassing api-gateway (as internal calls
 * do throughout this build) — sends the same shared secret the gateway stamps on requests, so
 * catalog-service's GatewaySecretFilter accepts it as a trusted internal caller.
 *
 * Every method is wrapped with a "catalog-service" circuit breaker and retry (both configured in
 * application.yml under resilience4j:). Resilience4j's @TimeLimiter only applies to methods
 * returning CompletableFuture, which would force this synchronous client's whole call chain
 * async — instead, the same configured duration bounds the underlying HTTP client's
 * connect/read timeout directly, so it stays a single config value either way.
 */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

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

    /**
     * storeId is the caller's currently-delivering-from store — forwarded so catalog-service can
     * report NOT_AVAILABLE_AT_STORE for a book stocked at a different store, rather than its raw
     * (irrelevant to this caller) inventory count. Null is a valid value (e.g. a qty-only cart
     * update where the item was already store-validated at add time).
     */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "getBookFallback")
    @Retry(name = "catalog-service")
    public BookInfo getBook(UUID bookId, UUID storeId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/books/{id}")
                        .queryParamIfPresent("storeId", Optional.ofNullable(storeId))
                        .build(bookId))
                .retrieve()
                .onStatus(this::isNotFound, (req, res) -> {
                    throw new BookNotFoundException();
                })
                .body(BookInfo.class);
    }

    @CircuitBreaker(name = "catalog-service", fallbackMethod = "reserveStockFallback")
    @Retry(name = "catalog-service")
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

    /** No stock reservation involved — a digital copy doesn't deplete, only its availability/price is looked up. */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "lookupVirtualEditionsFallback")
    @Retry(name = "catalog-service")
    public VirtualEditionLookupResponse lookupVirtualEditions(VirtualEditionLookupRequest request) {
        return restClient.post()
                .uri("/internal/virtual-editions/lookup")
                .body(request)
                .retrieve()
                .body(VirtualEditionLookupResponse.class);
    }

    /**
     * Best-effort, unlike the checkout-critical methods above: cover images are a display
     * enrichment for the order list, not something that should block the page from loading if
     * catalog-service is briefly unavailable — a missing thumbnail degrades gracefully, an order
     * page that won't load doesn't.
     */
    public Map<UUID, String> getCoverImageUrls(List<UUID> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        try {
            BookCoverLookupResponse response = restClient.post()
                    .uri("/internal/books/covers")
                    .body(new BookCoverLookupRequest(bookIds))
                    .retrieve()
                    .body(BookCoverLookupResponse.class);
            if (response == null) {
                return Map.of();
            }
            return response.items().stream()
                    .filter(item -> item.coverImageUrl() != null)
                    .collect(Collectors.toMap(BookCover::id, BookCover::coverImageUrl, (a, b) -> a));
        } catch (Exception e) {
            log.warn("Could not fetch cover images from catalog-service", e);
            return Map.of();
        }
    }

    private BookInfo getBookFallback(UUID bookId, UUID storeId, Throwable t) {
        throw translate(t);
    }

    private ReserveStockResponse reserveStockFallback(ReserveStockRequest request, Throwable t) {
        throw translate(t);
    }

    private VirtualEditionLookupResponse lookupVirtualEditionsFallback(VirtualEditionLookupRequest request, Throwable t) {
        throw translate(t);
    }

    /**
     * Converts a circuit-breaker-open rejection into the documented "unavailable" error; any
     * other failure (a business error like BookNotFoundException, or a real transient failure
     * once retries are exhausted) propagates unchanged.
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

    private boolean isNotFound(HttpStatusCode status) {
        return status.value() == 404;
    }

    private boolean isConflict(HttpStatusCode status) {
        return status.value() == 409;
    }
}
