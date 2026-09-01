package com.readora.catalog.client;

import com.readora.catalog.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Points at an unreachable address on the loopback range (nothing listens on port 1) so every
 * call fails fast with a connection error — exercises the degrade-gracefully-or-not behavior
 * documented on each method without needing a real HTTP server.
 */
class UserServiceClientTest {

    private UserServiceClient client;

    @BeforeEach
    void setUp() {
        client = new UserServiceClient("http://127.0.0.1:1", "gateway-secret");
    }

    @Test
    void getAdminStoreId_unreachable_throwsServiceExceptionRatherThanFailingOpen() {
        assertThatThrownBy(() -> client.getAdminStoreId(UUID.randomUUID()))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", "USER_SERVICE_UNAVAILABLE");
    }

    @Test
    void getDisplayName_unreachable_degradesToNull() {
        assertThat(client.getDisplayName(UUID.randomUUID())).isNull();
    }

    @Test
    void getRecentBookViewIds_unreachable_degradesToEmptyList() {
        assertThat(client.getRecentBookViewIds(UUID.randomUUID(), 20)).isEmpty();
    }

    @Test
    void getRecentSearchTerms_unreachable_degradesToEmptyList() {
        assertThat(client.getRecentSearchTerms(UUID.randomUUID(), 20)).isEmpty();
    }
}
