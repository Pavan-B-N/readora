package com.readora.mcp.client;

import com.readora.mcp.dto.CartInfo;
import com.readora.mcp.dto.OrderPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Forwards the caller's userId as X-User-Id — commerce-service trusts that header once
 * GatewaySecretFilter has proven the request came from a trusted internal caller (mcp-server is
 * one). Every tool call is scoped to whichever user the AI agent is acting on behalf of.
 */
@Component
public class CommerceClient {

    private final RestClient restClient;

    public CommerceClient(
            @Value("${app.commerce-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
    }

    public CartInfo getCart(String userId) {
        return restClient.get()
                .uri("/api/v1/cart")
                .header("X-User-Id", userId)
                .retrieve()
                .body(CartInfo.class);
    }

    public OrderPage getOrderHistory(String userId) {
        return restClient.get()
                .uri("/api/v1/orders")
                .header("X-User-Id", userId)
                .retrieve()
                .body(OrderPage.class);
    }
}
