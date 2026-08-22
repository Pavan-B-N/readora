package com.readora.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.gateway.config.RateLimitProperties;
import com.readora.gateway.config.RateLimitProperties.RateLimitRule;
import com.readora.gateway.dto.ErrorResponse;
import com.readora.gateway.filter.CorrelationIdGlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Runs after JwtAuthenticationGlobalFilter (order 0 > -1) so X-User-Id, if the request carried
 * a valid token, is already attached — that's what lets this key per authenticated user rather
 * than per IP. Which rule applies is looked up by the matched route's id, so a new route (e.g.
 * ai-service) just needs an entry under app.rate-limit.rules in application.yml — no code change.
 */
@Component
public class RateLimitingGlobalFilter implements GlobalFilter, Ordered {

    private static final String DEFAULT_ROUTE_ID = "default";

    private final RedisRateLimiterService rateLimiterService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitingGlobalFilter(
            RedisRateLimiterService rateLimiterService,
            RateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        this.rateLimiterService = rateLimiterService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String routeId = resolveRouteId(exchange);
        RateLimitRule rule = properties.ruleFor(routeId);

        String key = "ratelimit:%s:%s".formatted(routeId, resolveKey(exchange));

        return rateLimiterService.isAllowed(key, rule)
                .flatMap(allowed -> allowed ? chain.filter(exchange) : reject(exchange, rule));
    }

    private String resolveRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : DEFAULT_ROUTE_ID;
    }

    private String resolveKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return userId;
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    private Mono<Void> reject(ServerWebExchange exchange, RateLimitRule rule) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.RETRY_AFTER, String.valueOf(rule.windowSeconds()));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CorrelationIdGlobalFilter.CORRELATION_ID_HEADER);

        ErrorResponse body = new ErrorResponse(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Try again later.",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                exchange.getRequest().getURI().getPath(),
                correlationId,
                Instant.now()
        );

        byte[] bytes = writeBody(body);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private byte[] writeBody(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            return "{\"error\":\"RATE_LIMIT_EXCEEDED\"}".getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
