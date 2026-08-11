package com.readora.ai.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Points at an unreachable address so calls fail fast with a connection error. The
 * @CircuitBreaker annotations are no-ops without a Spring AOP proxy in a plain unit test, so
 * these exercise the underlying RestClient call path, not the fallback/translate() logic.
 */
class CatalogClientTest {

    private CatalogClient client;

    @BeforeEach
    void setUp() {
        client = new CatalogClient("http://127.0.0.1:1", "gateway-secret", Duration.ofMillis(200));
    }

    @Test
    void listBooksNeedingReembedding_unreachable_throws() {
        assertThatThrownBy(() -> client.listBooksNeedingReembedding(50)).isInstanceOf(Exception.class);
    }

    @Test
    void markEmbedded_emptyList_isANoOpWithoutCallingOut() {
        client.markEmbedded(List.of());
        // No exception, and no HTTP call attempted — nothing further to assert.
    }

    @Test
    void markEmbedded_unreachable_throws() {
        assertThatThrownBy(() -> client.markEmbedded(List.of(UUID.randomUUID()))).isInstanceOf(Exception.class);
    }

    @Test
    void lookupBooks_unreachable_throws() {
        assertThatThrownBy(() -> client.lookupBooks(List.of(UUID.randomUUID()))).isInstanceOf(Exception.class);
    }

    @Test
    void isOwned_unreachable_throws() {
        assertThatThrownBy(() -> client.isOwned(UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(Exception.class);
    }

    @Test
    void getBookContent_unreachable_throws() {
        assertThatThrownBy(() -> client.getBookContent(UUID.randomUUID())).isInstanceOf(Exception.class);
    }

    @Test
    void checkAvailability_emptyBookIds_returnsEmptyWithoutCallingOut() {
        assertThat(client.checkAvailability(List.of(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void checkAvailability_unreachable_throws() {
        assertThatThrownBy(() -> client.checkAvailability(List.of(UUID.randomUUID()), UUID.randomUUID()))
                .isInstanceOf(Exception.class);
    }
}
