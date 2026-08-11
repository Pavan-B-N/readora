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

// Direct service-to-service calls to catalog-service; every method is wrapped with a "catalog-service" circuit breaker (configured in application.yml under resilience4j:) — no retry here since listBooksNeedingReembedding and markEmbedded run in a loop inside a Kafka consumer where retrying would stall the partition (the Kafka retry/DLQ mechanism handles that instead), and since Resilience4j's @TimeLimiter only applies to CompletableFuture-returning methods, the same configured duration instead bounds the underlying HTTP client's connect/read timeout directly, keeping it a single config value either way.
@Component
public class CatalogClient {

    private final RestClient restClient;

    // Builds the RestClient with the gateway secret header and a timeout-bound request factory.
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

    // Pulled in for the full backfill — title, authors, description, and table of contents, scoped to books never embedded or changed since; always page 0 since the backfill marks each returned batch embedded immediately after processing, shrinking this same filtered set out from under a page-by-page walk otherwise.
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

    // Fetches one page of book export data from catalog-service, optionally filtered to books needing re-embedding.
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

    // Gates both indexing and chatting about a book's content — only a real purchaser may trigger either.
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

    // Circuit-breaker fallback for isOwned; translates the failure into the standard unavailable error.
    private boolean isOwnedFallback(UUID userId, UUID bookId, Throwable t) {
        throw translate(t);
    }

    // Circuit-breaker fallback for getBookContent; translates the failure into the standard unavailable error.
    private byte[] getBookContentFallback(UUID bookId, Throwable t) {
        throw translate(t);
    }

    // Response body wrapping the ownership flag.
    private record OwnedResponse(boolean owned) {
    }

    // Filters bookIds down to ones actually purchasable at storeId (virtual, or in stock at that store) — the enforcement point behind the book-recommendation tools' store guardrail; fails closed on a catalog-service outage, since an empty result ("recommend nothing") is safer than falling back to "everything is available" and defeating the guardrail's purpose.
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

    // Circuit-breaker fallback for checkAvailability; fails closed to an empty (nothing available) list.
    private List<UUID> checkAvailabilityFallback(List<UUID> bookIds, UUID storeId, Throwable t) {
        return List.of();
    }

    // Circuit-breaker fallback for listBooksNeedingReembedding; translates the failure into the standard unavailable error.
    private List<BookDoc> listBooksNeedingReembeddingFallback(int size, Throwable t) {
        throw translate(t);
    }

    // Circuit-breaker fallback for markEmbedded; translates the failure into the standard unavailable error.
    private void markEmbeddedFallback(List<UUID> bookIds, Throwable t) {
        throw translate(t);
    }

    // Circuit-breaker fallback for lookupBooks; translates the failure into the standard unavailable error.
    private List<BookDoc> lookupBooksFallback(List<UUID> bookIds, Throwable t) {
        throw translate(t);
    }

    // Converts a circuit-breaker-open rejection into the documented "unavailable" error; any other failure propagates unchanged.
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

    // Response body for a page of the book export endpoint.
    private record BookExportPageResponse(List<BookDoc> items, int totalPages) {
    }

    // Request body for marking books embedded.
    private record MarkEmbeddedRequest(List<UUID> bookIds) {
    }

    // Request body for looking up books by id.
    private record BookLookupRequest(List<UUID> bookIds) {
    }

    // Response body for a book lookup request.
    private record BookLookupResponse(List<BookDoc> items) {
    }

    // Request body for checking store availability of books.
    private record AvailabilityRequest(List<UUID> bookIds, UUID storeId) {
    }

    // Response body listing which book ids are available.
    private record AvailabilityResponse(List<UUID> availableBookIds) {
    }
}
