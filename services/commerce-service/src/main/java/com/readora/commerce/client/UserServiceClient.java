package com.readora.commerce.client;

import com.readora.commerce.dto.WalletBalance;
import com.readora.commerce.exception.ServiceException;
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

/**
 * Direct service-to-service calls to user-service, bypassing api-gateway — same pattern as
 * CatalogClient. Used at checkout to verify the caller's wallet balance synchronously, before an
 * order is persisted, so an under-funded checkout fails immediately with a clear reason rather
 * than creating a doomed order that fails payment asynchronously.
 */
@Component
public class UserServiceClient {

    private final RestClient restClient;

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

    @CircuitBreaker(name = "user-service", fallbackMethod = "getWalletBalanceFallback")
    @Retry(name = "user-service")
    public WalletBalance getWalletBalance(UUID userId) {
        return restClient.get()
                .uri("/internal/wallet/{userId}/balance", userId)
                .retrieve()
                .body(WalletBalance.class);
    }

    private WalletBalance getWalletBalanceFallback(UUID userId, Throwable t) {
        throw translate(t);
    }

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
