package com.readora.catalog.client;

import com.readora.catalog.dto.AdminStoreResponse;
import com.readora.catalog.dto.DisplayNameResponse;
import com.readora.catalog.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Direct service-to-service call to user-service, bypassing api-gateway — same pattern as
 * CommerceClient. Unlike CommerceClient this backs a security check (server-side admin
 * store-scoping), so failures must not fail open: any error here is surfaced as a 503 rather than
 * silently treated as "no store assigned."
 */
@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestClient restClient;

    public UserServiceClient(
            @Value("${app.user-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    /** @return the store the admin is assigned to, or null if they're an admin with no assignment yet */
    public UUID getAdminStoreId(UUID userId) {
        try {
            AdminStoreResponse response = restClient.get()
                    .uri("/internal/admin-users/{userId}/store", userId)
                    .retrieve()
                    .body(AdminStoreResponse.class);
            return response != null ? response.storeId() : null;
        } catch (Exception e) {
            throw new ServiceException(
                    "USER_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not verify your store assignment — try again shortly"
            );
        }
    }

    /**
     * Best-effort, unlike getAdminStoreId — a display name is cosmetic, not a security check, so
     * a failure degrades to null (reviews fall back to "Anonymous") rather than blocking the write.
     */
    public String getDisplayName(UUID userId) {
        try {
            DisplayNameResponse response = restClient.get()
                    .uri("/internal/profiles/{userId}/display-name", userId)
                    .retrieve()
                    .body(DisplayNameResponse.class);
            return response != null ? response.displayName() : null;
        } catch (Exception e) {
            log.warn("Could not fetch display name from user-service for review authoring", e);
            return null;
        }
    }

    /** Best-effort — recommendations degrade to fewer signals rather than failing if user-service is unreachable. */
    public List<UUID> getRecentBookViewIds(UUID userId, int limit) {
        try {
            List<UUID> response = restClient.get()
                    .uri("/internal/profiles/{userId}/recent-book-views?limit={limit}", userId, limit)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UUID>>() {
                    });
            return response != null ? response : List.of();
        } catch (Exception e) {
            log.warn("Could not fetch recent book views from user-service for recommendations", e);
            return List.of();
        }
    }

    /** Best-effort — recommendations degrade to fewer signals rather than failing if user-service is unreachable. */
    public List<String> getRecentSearchTerms(UUID userId, int limit) {
        try {
            List<String> response = restClient.get()
                    .uri("/internal/profiles/{userId}/recent-searches?limit={limit}", userId, limit)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {
                    });
            return response != null ? response : List.of();
        } catch (Exception e) {
            log.warn("Could not fetch recent search terms from user-service for recommendations", e);
            return List.of();
        }
    }
}
