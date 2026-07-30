package com.readora.commerce.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Points at an unreachable address so every call fails fast with a connection error. The
 * @CircuitBreaker annotations are no-ops without a Spring AOP proxy, so getAdminUserIdForStore's
 * fallback (which returns null) isn't exercised here either — only its direct call path is.
 */
class UserServiceClientTest {

    private UserServiceClient client;

    @BeforeEach
    void setUp() {
        client = new UserServiceClient("http://127.0.0.1:1", "gateway-secret", Duration.ofMillis(200));
    }

    @Test
    void getWalletBalance_unreachable_throws() {
        assertThatThrownBy(() -> client.getWalletBalance(UUID.randomUUID())).isInstanceOf(Exception.class);
    }

    @Test
    void getAdminStoreId_unreachable_throws() {
        assertThatThrownBy(() -> client.getAdminStoreId(UUID.randomUUID())).isInstanceOf(Exception.class);
    }

    @Test
    void getAdminUserIdForStore_unreachable_throws() {
        assertThatThrownBy(() -> client.getAdminUserIdForStore(UUID.randomUUID())).isInstanceOf(Exception.class);
    }
}
