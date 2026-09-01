package com.readora.commerce.client;

import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.VirtualEditionLookupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Points at an unreachable address on the loopback range so every call fails fast with a
 * connection error. The @CircuitBreaker/@Retry annotations are no-ops here (no Spring AOP proxy
 * in a plain unit test), so these exercise the underlying RestClient call path, not the
 * fallback/translate() logic — that needs a full Spring context, out of scope for a unit test.
 */
class CatalogClientTest {

    private CatalogClient client;

    @BeforeEach
    void setUp() {
        client = new CatalogClient("http://127.0.0.1:1", "gateway-secret", Duration.ofMillis(200));
    }

    @Test
    void getBook_unreachable_throws() {
        assertThatThrownBy(() -> client.getBook(UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(Exception.class);
    }

    @Test
    void getStore_unreachable_throws() {
        assertThatThrownBy(() -> client.getStore(UUID.randomUUID())).isInstanceOf(Exception.class);
    }

    @Test
    void reserveStock_unreachable_throws() {
        assertThatThrownBy(() -> client.reserveStock(
                new ReserveStockRequest(List.of(new ReserveStockRequest.Item(UUID.randomUUID(), 1)))))
                .isInstanceOf(Exception.class);
    }

    @Test
    void lookupVirtualEditions_unreachable_throws() {
        assertThatThrownBy(() -> client.lookupVirtualEditions(new VirtualEditionLookupRequest(List.of(UUID.randomUUID()))))
                .isInstanceOf(Exception.class);
    }

    @Test
    void getCoverImageUrls_emptyInput_returnsEmptyWithoutCallingOut() {
        assertThat(client.getCoverImageUrls(List.of())).isEmpty();
    }

    @Test
    void getCoverImageUrls_unreachable_degradesToEmptyMap() {
        assertThat(client.getCoverImageUrls(List.of(UUID.randomUUID()))).isEmpty();
    }
}
