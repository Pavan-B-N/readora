package com.readora.commerce.client;

import com.readora.commerce.dto.ReserveStockRequest;
import com.readora.commerce.dto.VirtualEditionLookupRequest;
import com.readora.commerce.exception.BookNotFoundException;
import com.readora.commerce.exception.InsufficientStockException;
import com.readora.commerce.exception.StoreNotFoundException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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

    @org.junit.jupiter.api.Nested
    class AgainstARealServer {

        private HttpServer server;
        private CatalogClient realClient;

        @BeforeEach
        void startServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/books/", exchange -> respond(exchange, 404, "{}"));
            server.createContext("/internal/stores/", exchange -> respond(exchange, 404, "{}"));
            server.createContext("/internal/inventory/reserve", exchange -> respond(exchange, 409, "{}"));
            server.start();
            realClient = new CatalogClient(
                    "http://127.0.0.1:" + server.getAddress().getPort(), "gateway-secret", Duration.ofSeconds(2)
            );
        }

        @AfterEach
        void stopServer() {
            server.stop(0);
        }

        private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Test
        void getBook_serverReturns404_throwsBookNotFoundException() {
            assertThatThrownBy(() -> realClient.getBook(UUID.randomUUID(), null))
                    .isInstanceOf(BookNotFoundException.class);
        }

        @Test
        void getStore_serverReturns404_throwsStoreNotFoundException() {
            assertThatThrownBy(() -> realClient.getStore(UUID.randomUUID()))
                    .isInstanceOf(StoreNotFoundException.class);
        }

        @Test
        void reserveStock_serverReturns409_throwsInsufficientStockException() {
            assertThatThrownBy(() -> realClient.reserveStock(
                    new ReserveStockRequest(List.of(new ReserveStockRequest.Item(UUID.randomUUID(), 1)))))
                    .isInstanceOf(InsufficientStockException.class);
        }
    }
}
