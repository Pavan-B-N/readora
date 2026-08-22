package com.readora.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Adds the gateway secret to requests forwarded to downstream services. */
@Component
public class GatewaySecretGlobalFilter implements GlobalFilter, Ordered {

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final String gatewaySecret;

    public GatewaySecretGlobalFilter(@Value("${app.gateway.secret}") String gatewaySecret) {
        this.gatewaySecret = gatewaySecret;
    }

    /**
     * Adds the gateway secret header and forwards the modified request.
     *
     * @param exchange the current HTTP request and response exchange
     * @param chain the remaining gateway filter chain
     * @return a Mono that completes when gateway processing finishes
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(GATEWAY_SECRET_HEADER, gatewaySecret)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Defines this filter's execution order in the gateway filter chain.
     *
     * @return the filter execution order
     */
    @Override
    public int getOrder() {
        return -3;
    }
}