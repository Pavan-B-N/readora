package com.readora.commerce.client;

import com.readora.commerce.dto.AdminStoreResponse;
import com.readora.commerce.dto.StoreAdminResponse;
import com.readora.commerce.dto.WalletBalance;
import com.readora.sharedcore.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

// Direct service-to-service calls to user-service, bypassing api-gateway — same pattern as CatalogClient; used at checkout to verify the caller's wallet balance synchronously before an order is persisted, so an under-funded checkout fails immediately rather than creating a doomed order that fails payment asynchronously.
@Component
public class UserServiceClient {

    private final RestClient restClient;

    // Builds the internal RestClient with the gateway secret header and configured timeout.
    public UserServiceClient(
            @Value("${app.user-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret,
            @Value("${resilience4j.timelimiter.instances.user-service.timeout-duration}") Duration timeout
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

    // Fetches the caller's current wallet balance, used to validate funds synchronously at checkout.
    @CircuitBreaker(name = "user-service", fallbackMethod = "getWalletBalanceFallback")
    @Retry(name = "user-service")
    public WalletBalance getWalletBalance(UUID userId) {
        return restClient.get()
                .uri("/internal/wallet/{userId}/balance", userId)
                .retrieve()
                .body(WalletBalance.class);
    }

    // Circuit-breaker fallback for getWalletBalance; translates the failure via translate().
    private WalletBalance getWalletBalanceFallback(UUID userId, Throwable t) {
        throw translate(t);
    }

    // Resolves which store an admin is scoped to — same fail-closed semantics as catalog-service's identical method: an error here must never be treated as "no store."
    @CircuitBreaker(name = "user-service", fallbackMethod = "getAdminStoreIdFallback")
    @Retry(name = "user-service")
    public UUID getAdminStoreId(UUID userId) {
        AdminStoreResponse response = restClient.get()
                .uri("/internal/admin-users/{userId}/store", userId)
                .retrieve()
                .body(AdminStoreResponse.class);
        return response != null ? response.storeId() : null;
    }

    // Circuit-breaker fallback for getAdminStoreId; fails closed by rethrowing via translate().
    private UUID getAdminStoreIdFallback(UUID userId, Throwable t) {
        throw translate(t);
    }

    // The reverse of getAdminStoreId() — who to notify about a return at this store. Best-effort: a lookup failure just means the admin misses one notification, not a reason to fail the customer-facing action that triggered it.
    @CircuitBreaker(name = "user-service", fallbackMethod = "getAdminUserIdForStoreFallback")
    public UUID getAdminUserIdForStore(UUID storeId) {
        StoreAdminResponse response = restClient.get()
                .uri("/internal/admin-users/by-store/{storeId}", storeId)
                .retrieve()
                .body(StoreAdminResponse.class);
        return response != null ? response.userId() : null;
    }

    // Circuit-breaker fallback for getAdminUserIdForStore; fails open (returns null) since notifying the admin is best-effort.
    private UUID getAdminUserIdForStoreFallback(UUID storeId, Throwable t) {
        return null;
    }

    // Converts a circuit-breaker-open rejection into the documented "unavailable" error; other failures propagate unchanged.
    private RuntimeException translate(Throwable t) {
        if (t instanceof CallNotPermittedException) {
            return new ServiceException(
                    "USER_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "User service is currently unavailable"
            );
        }
        if (t instanceof RuntimeException re) {
            return re;
        }
        return new ServiceException(
                "USER_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "User service is currently unavailable"
        );
    }
}
