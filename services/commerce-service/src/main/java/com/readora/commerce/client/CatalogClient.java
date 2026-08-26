package com.readora.commerce.client;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

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

    @CircuitBreaker(name = "catalog-service", fallbackMethod = "getBookFallback")
    @Retry(name = "catalog-service")
    public BookInfo getBook(UUID bookId) {
        return restClient.get()
                .uri("/api/v1/books/{id}", bookId)
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

    private BookInfo getBookFallback(UUID bookId, Throwable t) {
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
