package com.readora.mcp.client;

import com.readora.mcp.dto.CartInfo;
import com.readora.mcp.dto.OrderPage;
import com.readora.sharedcore.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Forwards the caller's userId as a freshly minted internal JWT — commerce-service validates it
 * itself and extracts the userId, the same as it would for a real user token. Every tool call is
 * scoped to whichever user the AI agent is acting on behalf of.
 */
@Component
public class CommerceClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    public CommerceClient(
            @Value("${app.commerce-service.base-url}") String baseUrl,
            @Value("${app.gateway.secret}") String gatewaySecret,
            JwtService jwtService
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Secret", gatewaySecret)
                .build();
        this.jwtService = jwtService;
    }

    public CartInfo getCart(String userId) {
        return restClient.get()
                .uri("/api/v1/cart")
                .header("Authorization", "Bearer " + jwtService.issueInternalToken(userId))
                .retrieve()
                .body(CartInfo.class);
    }

    public OrderPage getOrderHistory(String userId) {
        return restClient.get()
                .uri("/api/v1/orders")
                .header("Authorization", "Bearer " + jwtService.issueInternalToken(userId))
                .retrieve()
                .body(OrderPage.class);
    }
}
