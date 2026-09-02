package com.readora.mcp.client;

import com.readora.sharedcore.security.JwtService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserClientTest {

    private UserClient unreachableClient;
    private JwtService jwtService;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                io.jsonwebtoken.security.Keys.hmacShaKeyFor("a-test-only-secret-that-is-long-enough-for-hs256".getBytes()).getEncoded());
        jwtService = new JwtService(secret);
        unreachableClient = new UserClient("http://127.0.0.1:1", "gateway-secret", jwtService);
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getProfile_unreachable_throws() {
        assertThatThrownBy(() -> unreachableClient.getProfile("u1")).isInstanceOf(Exception.class);
    }

    @Test
    void getWallet_unreachable_throws() {
        assertThatThrownBy(() -> unreachableClient.getWallet("u1")).isInstanceOf(Exception.class);
    }

    @Test
    void getWallet_realResponse_deserializesIntoWalletInfo() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/users/me/wallet", exchange -> {
            byte[] body = "{\"balance\":500.00,\"currency\":\"INR\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        UserClient client = new UserClient("http://127.0.0.1:" + server.getAddress().getPort(), "gateway-secret", jwtService);

        var wallet = client.getWallet("u1");

        assertThat(wallet.currency()).isEqualTo("INR");
    }
}
