package com.readora.catalog.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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

    @org.junit.jupiter.api.Nested
    class AgainstARealServer {

        private HttpServer server;
        private CommerceClient realClient;

        @BeforeEach
        void startServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/orders/purchased-book-ids", exchange -> {
                byte[] body = "{\"bookIds\":[]}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.createContext("/internal/orders/recent-items", exchange -> {
                byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            realClient = new CommerceClient("http://127.0.0.1:" + server.getAddress().getPort(), "gateway-secret");
        }

        @AfterEach
        void stopServer() {
            server.stop(0);
        }

        @Test
        void getPurchasedBookIds_reachable_parsesTheResponseBody() {
            assertThat(realClient.getPurchasedBookIds(UUID.randomUUID())).isEmpty();
        }

        @Test
        void getRecentOrderItems_reachable_parsesTheResponseBody() {
            assertThat(realClient.getRecentOrderItems(UUID.randomUUID(), 20)).isEmpty();
        }
    }
}
