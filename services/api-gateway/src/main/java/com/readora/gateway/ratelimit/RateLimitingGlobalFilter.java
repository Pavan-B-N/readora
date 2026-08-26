package com.readora.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.gateway.config.RateLimitProperties;
import com.readora.gateway.config.RateLimitProperties.RateLimitRule;
import com.readora.gateway.dto.ErrorResponse;
import com.readora.gateway.filter.CorrelationIdGlobalFilter;
import com.readora.gateway.security.JwtService;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Enforces Redis-backed rate limits using route-specific rules and user ID or client IP keys.
 * Returns 429 Too Many Requests when a request exceeds its configured limit.
 */
@Component
public class RateLimitingGlobalFilter implements GlobalFilter, Ordered {

    private static final String DEFAULT_ROUTE_ID = "default";

    private final RedisRateLimiterService rateLimiterService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    public RateLimitingGlobalFilter(
            RedisRateLimiterService rateLimiterService,
            RateLimitProperties properties,
            ObjectMapper objectMapper,
            JwtService jwtService
    ) {
        this.rateLimiterService = rateLimiterService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    /**
     * Enforces the rate limit for the matched route, keyed by the caller's user id (if
     * authenticated) or IP address otherwise.
     *
     * @param exchange the current HTTP request and response exchange
     * @param chain    the remaining gateway filter chain
     * @return a Mono that completes when the request is forwarded, or rejected with 429 if the
     *         route's rate limit has been exceeded
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String routeId = resolveRouteId(exchange);
        RateLimitRule rule = properties.ruleFor(routeId);

        String key = "ratelimit:%s:%s".formatted(routeId, resolveKey(exchange));

        return rateLimiterService.isAllowed(key, rule)
                .flatMap(allowed -> allowed ? chain.filter(exchange) : reject(exchange, rule));
    }

    /**
     * Reads which route matched this request, so rate-limit rules can be looked up per route.
     *
     * @param exchange the current HTTP request and response exchange
     * @return the matched route's id, or {@link #DEFAULT_ROUTE_ID} if no route was matched
     */
    private String resolveRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : DEFAULT_ROUTE_ID;
    }

    /**
     * Resolves who to rate-limit the request against: the authenticated user id if present,
     * otherwise the caller's IP address.
     *
     * @param exchange the current HTTP request and response exchange
     * @return the user id if the request is authenticated, else the caller's IP, else "unknown"
     */
    private String resolveKey(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            Optional<UUID> userId = jwtService.extractUserId(header.substring(7));
            if (userId.isPresent()) {
                return userId.get().toString();
            }
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Writes a 429 response in the shared error envelope shape, with a Retry-After header set
     * to the rule's window so the caller knows when it's safe to retry.
     *
     * @param exchange the current HTTP request and response exchange, used to write the response
     * @param rule     the rate-limit rule that was exceeded, used for the Retry-After value
     * @return a Mono that completes once the rejection body has been written
     */
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

    /**
     * Serializes the error body to JSON, falling back to a minimal hand-written JSON string if
     * serialization itself fails — the rejection response must never throw.
     *
     * @param body the error response to serialize
     * @return the JSON-encoded body as bytes
     */
    private byte[] writeBody(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            return "{\"error\":\"RATE_LIMIT_EXCEEDED\"}".getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Runs after the correlation-id, gateway-secret, and JWT filters (all negative orders), so
     * the Authorization header has already been validated by the time this filter resolves its
     * rate-limit key.
     *
     * @return this filter's order in the gateway filter chain
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
