package com.readora.catalog.client;

import com.readora.catalog.dto.PurchasedBookIdsResponse;
import com.readora.catalog.dto.RecentOrderItemResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Direct service-to-service call to commerce-service, bypassing api-gateway — same pattern as
 * other internal clients in this build. Used only for recommendations, a best-effort feature —
 * deliberately no circuit breaker/retry here: on any failure this just returns no purchase
 * history, and the caller falls back to showing no recommendations rather than an error.
 */
@Component
public class CommerceClient {

    private static final Logger log = LoggerFactory.getLogger(CommerceClient.class);

    private final RestClient restClient;

    public CommerceClient(
            @Value("${app.commerce-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    public List<UUID> getPurchasedBookIds(UUID userId) {
        try {
            PurchasedBookIdsResponse response = restClient.get()
                    .uri("/internal/orders/purchased-book-ids?userId={userId}", userId)
                    .retrieve()
                    .body(PurchasedBookIdsResponse.class);
            return response != null ? response.bookIds() : List.of();
        } catch (Exception e) {
            log.warn("Could not fetch purchase history from commerce-service for recommendations", e);
            return List.of();
        }
    }

    /** Backs the "Your orders" rail — best-effort, same reasoning as getPurchasedBookIds: empty on failure, not an error. */
    public List<RecentOrderItemResponse> getRecentOrderItems(UUID userId, int limit) {
        try {
            RecentOrderItemResponse[] response = restClient.get()
                    .uri("/internal/orders/recent-items?userId={userId}&limit={limit}", userId, limit)
                    .retrieve()
                    .body(RecentOrderItemResponse[].class);
            return response != null ? List.of(response) : List.of();
        } catch (Exception e) {
            log.warn("Could not fetch recent orders from commerce-service", e);
            return List.of();
        }
    }
}
