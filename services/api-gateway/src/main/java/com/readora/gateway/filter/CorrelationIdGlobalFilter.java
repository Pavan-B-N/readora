package com.readora.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Generates and propagates a correlation ID for each gateway request. */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /**
     * Adds a correlation ID to the forwarded request and response.
     *
     * @param exchange the current HTTP request and response exchange
     * @param chain the remaining gateway filter chain
     * @return a Mono that completes when gateway processing finishes
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = UUID.randomUUID().toString();

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Defines this filter's execution order in the gateway filter chain.
     *
     * @return the filter execution order
     */
    @Override
    public int getOrder() {
        return -2;
    }
}
