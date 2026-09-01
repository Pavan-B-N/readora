package com.readora.catalog.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Points at an unreachable address on the loopback range (nothing listens on port 1) so every
 * call fails fast with a connection error — exercises the best-effort degrade-to-empty behavior
 * documented on this class without needing a real HTTP server.
 */
class CommerceClientTest {

    private CommerceClient client;

    @BeforeEach
    void setUp() {
        client = new CommerceClient("http://127.0.0.1:1", "gateway-secret");
    }

    @Test
    void getPurchasedBookIds_unreachable_degradesToEmptyList() {
        assertThat(client.getPurchasedBookIds(UUID.randomUUID())).isEmpty();
    }

    @Test
    void getRecentOrderItems_unreachable_degradesToEmptyList() {
        assertThat(client.getRecentOrderItems(UUID.randomUUID(), 20)).isEmpty();
    }
}
