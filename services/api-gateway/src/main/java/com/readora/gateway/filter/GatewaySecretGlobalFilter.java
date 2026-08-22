package com.readora.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Stamps every proxied request with the shared secret auth-service (and future downstream
 * services) checks for in GatewaySecretFilter — proof the request actually came through this
 * gateway, not a direct hit on the service's own port. Runs first (lowest order) so every other
 * filter's forwarded request already carries it.
 */
@Component
public class GatewaySecretGlobalFilter implements GlobalFilter, Ordered {

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final String gatewaySecret;

    public GatewaySecretGlobalFilter(@Value("${app.gateway.secret}") String gatewaySecret) {
        this.gatewaySecret = gatewaySecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(GATEWAY_SECRET_HEADER, gatewaySecret)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -3;
    }
}