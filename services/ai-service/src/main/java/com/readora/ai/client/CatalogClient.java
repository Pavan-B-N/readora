package com.readora.ai.client;

import com.readora.ai.dto.BookDoc;
import com.readora.sharedcore.exception.ServiceException;
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
 * Every method is wrapped with a "catalog-service" circuit breaker (configured in
 * application.yml under resilience4j:). No retry here: listBooksNeedingReembedding and
 * markEmbedded run in a loop inside a Kafka consumer (the backfill job listener), and retrying
 * inside the consumer would stall the partition — the Kafka retry/DLQ mechanism handles that at
 * a higher level instead. Resilience4j's @TimeLimiter only applies to methods returning
 * CompletableFuture, which would force this synchronous client async — instead, the same
 * configured duration bounds the underlying HTTP client's connect/read timeout directly, so it
 * stays a single config value either way.
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

    /**
     * Pulled in for the full backfill — title, authors, description, and table of contents, but
     * scoped to books that have never been embedded or whose content changed since their last
     * embedding. Always page 0: the backfill marks each returned batch embedded immediately
     * after processing it, which shrinks this same filtered set out from under a page-by-page
     * walk otherwise.
     */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "listBooksNeedingReembeddingFallback")
    public List<BookDoc> listBooksNeedingReembedding(int size) {
        return exportBooks(0, size, true);
    }

    /** Tells catalog-service these books were just successfully (re-)embedded, so a later backfill run skips them unless they change again. */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "markEmbeddedFallback")
    public void markEmbedded(List<UUID> bookIds) {
        if (bookIds.isEmpty()) return;
        restClient.post()
                .uri("/internal/books/embedded")
                .body(new MarkEmbeddedRequest(bookIds))
                .retrieve()
                .toBodilessEntity();
    }

    private List<BookDoc> exportBooks(int page, int size, boolean needsReembeddingOnly) {
        BookExportPageResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/books/export")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("needsReembeddingOnly", needsReembeddingOnly)
                        .build())
                .retrieve()
                .body(BookExportPageResponse.class);

        return response != null ? response.items() : List.of();
    }

    /** Pulled in for the incremental embedding consumer — same fields as the backfill export, but for specific book ids only. */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "lookupBooksFallback")
    public List<BookDoc> lookupBooks(List<UUID> bookIds) {
        BookLookupResponse response = restClient.post()
                .uri("/internal/books/lookup")
                .body(new BookLookupRequest(bookIds))
                .retrieve()
                .body(BookLookupResponse.class);

        return response != null ? response.items() : List.of();
    }

    /**
     * Filters bookIds down to ones actually purchasable at storeId (virtual, or in stock at that
     * store) — the enforcement point behind the book-recommendation tools' store guardrail. Fails
     * closed on a catalog-service outage: an empty result means "recommend nothing" rather than
     * risking a stale/wrong availability list, since silently falling back to "everything is
     * available" would defeat the guardrail's whole purpose.
     */
    /** Gates both indexing and chatting about a book's content — only a real purchaser may trigger either. */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "isOwnedFallback")
    public boolean isOwned(UUID userId, UUID bookId) {
        OwnedResponse response = restClient.get()
                .uri("/internal/books/{bookId}/owned?userId={userId}", bookId, userId)
                .retrieve()
                .body(OwnedResponse.class);
        return response != null && response.owned();
    }

    /** Raw bytes of a virtual edition's file, for the reader's text-extraction + embedding pipeline. */
    @CircuitBreaker(name = "catalog-service", fallbackMethod = "getBookContentFallback")
    public byte[] getBookContent(UUID bookId) {
        return restClient.get()
                .uri("/internal/books/{bookId}/content", bookId)
                .retrieve()
                .body(byte[].class);
    }

    private boolean isOwnedFallback(UUID userId, UUID bookId, Throwable t) {
        throw translate(t);
    }

    private byte[] getBookContentFallback(UUID bookId, Throwable t) {
        throw translate(t);
    }

    private record OwnedResponse(boolean owned) {
    }

    @CircuitBreaker(name = "catalog-service", fallbackMethod = "checkAvailabilityFallback")
    public List<UUID> checkAvailability(List<UUID> bookIds, UUID storeId) {
        if (bookIds.isEmpty()) {
            return List.of();
        }
        AvailabilityResponse response = restClient.post()
                .uri("/internal/books/availability")
                .body(new AvailabilityRequest(bookIds, storeId))
                .retrieve()
                .body(AvailabilityResponse.class);

        return response != null ? response.availableBookIds() : List.of();
    }

    private List<UUID> checkAvailabilityFallback(List<UUID> bookIds, UUID storeId, Throwable t) {
        return List.of();
    }

    private List<BookDoc> listBooksNeedingReembeddingFallback(int size, Throwable t) {
        throw translate(t);
    }

    private void markEmbeddedFallback(List<UUID> bookIds, Throwable t) {
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

    private record MarkEmbeddedRequest(List<UUID> bookIds) {
    }

    private record BookLookupRequest(List<UUID> bookIds) {
    }

    private record BookLookupResponse(List<BookDoc> items) {
    }

    private record AvailabilityRequest(List<UUID> bookIds, UUID storeId) {
    }

    private record AvailabilityResponse(List<UUID> availableBookIds) {
    }
}
