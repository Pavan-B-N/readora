package com.readora.delivery.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.sharedcore.exception.ServiceException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommerceClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getDeliveryDetail_unreachable_throwsServiceUnavailable() {
        CommerceClient client = new CommerceClient("http://127.0.0.1:1", "secret", new ObjectMapper());

        assertThatThrownBy(() -> client.getDeliveryDetail(UUID.randomUUID()))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COMMERCE_SERVICE_UNAVAILABLE");
    }

    @Test
    void updateDeliveryStatus_unreachable_throwsServiceUnavailable() {
        CommerceClient client = new CommerceClient("http://127.0.0.1:1", "secret", new ObjectMapper());

        assertThatThrownBy(() -> client.updateDeliveryStatus(UUID.randomUUID(), "ASSIGNED", UUID.randomUUID(), "Agent"))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COMMERCE_SERVICE_UNAVAILABLE");
    }

    @Test
    void updateReturnStatus_unreachable_throwsServiceUnavailable() {
        CommerceClient client = new CommerceClient("http://127.0.0.1:1", "secret", new ObjectMapper());

        assertThatThrownBy(() -> client.updateReturnStatus(UUID.randomUUID(), "RETURN_ASSIGNED", UUID.randomUUID(), "Agent"))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COMMERCE_SERVICE_UNAVAILABLE");
    }

    @Test
    void updateDeliveryStatus_upstreamRejection_passesThroughStatusAndMessage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "{\"message\":\"Order isn't in ASSIGNED status\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(409, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        CommerceClient client = new CommerceClient("http://127.0.0.1:" + server.getAddress().getPort(), "secret", new ObjectMapper());

        assertThatThrownBy(() -> client.updateDeliveryStatus(UUID.randomUUID(), "DELIVERED", UUID.randomUUID(), "Agent"))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COMMERCE_SERVICE_REJECTED")
                .hasMessageContaining("Order isn't in ASSIGNED status");
    }

    @Test
    void updateDeliveryStatus_upstreamRejectionWithUnparsableBody_fallsBackToGenericMessage() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "not json".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(409, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        CommerceClient client = new CommerceClient("http://127.0.0.1:" + server.getAddress().getPort(), "secret", new ObjectMapper());

        assertThatThrownBy(() -> client.updateDeliveryStatus(UUID.randomUUID(), "DELIVERED", UUID.randomUUID(), "Agent"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Commerce service rejected the request");
    }
}
