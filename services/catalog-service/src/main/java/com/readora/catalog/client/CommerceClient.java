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

// Direct service-to-service call to commerce-service, bypassing api-gateway — used only for recommendations (best-effort), so deliberately has no circuit breaker/retry: any failure just returns no purchase history rather than an error.
@Component
public class CommerceClient {

    private static final Logger log = LoggerFactory.getLogger(CommerceClient.class);

    private final RestClient restClient;

    // Builds the RestClient with the gateway secret header attached to every request.
    public CommerceClient(
            @Value("${app.commerce-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    // Best-effort — returns the user's purchased book ids, or empty if commerce-service is unreachable.
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
