package com.readora.mcp.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogClientTest {

    private final CatalogClient unreachableClient = new CatalogClient("http://127.0.0.1:1", "gateway-secret");
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void search_unreachable_throws() {
        assertThatThrownBy(() -> unreachableClient.search("clean code")).isInstanceOf(Exception.class);
    }

    @Test
    void getDetail_unreachable_throws() {
        assertThatThrownBy(() -> unreachableClient.getDetail("b1")).isInstanceOf(Exception.class);
    }

    @Test
    void search_realResponse_deserializesIntoBookPage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/books", exchange -> {
            byte[] body = ("{\"items\":[{\"id\":\"b1\",\"title\":\"Clean Code\",\"authors\":[\"Robert Martin\"],"
                    + "\"listPrice\":499.00,\"currency\":\"INR\",\"availability\":\"IN_STOCK\"}]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        CatalogClient client = new CatalogClient("http://127.0.0.1:" + server.getAddress().getPort(), "gateway-secret");

        var page = client.search("clean code");

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).title()).isEqualTo("Clean Code");
    }
}
