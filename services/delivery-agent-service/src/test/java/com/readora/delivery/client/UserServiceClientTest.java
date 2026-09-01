package com.readora.delivery.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.exception.ServiceException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getAdminStoreId_unreachable_throwsServiceUnavailable() {
        UserServiceClient client = new UserServiceClient("http://127.0.0.1:1", "secret", new ObjectMapper());

        assertThatThrownBy(() -> client.getAdminStoreId(UUID.randomUUID()))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", "USER_SERVICE_UNAVAILABLE");
    }

    @Test
    void getAdminStoreId_upstreamRejection_passesThroughStatusAndMessage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "{\"message\":\"Account isn't assigned to a store\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        UserServiceClient client = new UserServiceClient("http://127.0.0.1:" + server.getAddress().getPort(), "secret", new ObjectMapper());

        assertThatThrownBy(() -> client.getAdminStoreId(UUID.randomUUID()))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", "USER_SERVICE_REJECTED")
                .hasMessageContaining("Account isn't assigned to a store");
    }
}
