package com.readora.gateway.filter;

import com.readora.gateway.config.SecurityProperties;
import com.readora.gateway.security.AuthenticatedPrincipal;
import com.readora.gateway.security.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/** Denies unauthenticated requests by default; only configured public routes bypass JWT validation. */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationGlobalFilter(JwtService jwtService, SecurityProperties securityProperties) {
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    /**
     * Enforces JWT authentication for protected gateway routes.
     *
     * @param exchange the current HTTP request and response exchange
     * @param chain the remaining gateway filter chain
     * @return a Mono that completes when the request is forwarded or rejected
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            return reject(exchange);
        }

        String token = header.substring(7);
        Optional<AuthenticatedPrincipal> principal = jwtService.validate(token);

        if (principal.isEmpty()) {
            return reject(exchange);
        }

        // WebFlux request objects are immutable so we need an mutatedRequest
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header("X-User-Id", principal.get().userId().toString());

        if (principal.get().email() != null) {
            requestBuilder.header("X-User-Email", principal.get().email());
        }

        if (!principal.get().roles().isEmpty()) {
            requestBuilder.header("X-User-Roles", String.join(",", principal.get().roles()));
        }

        ServerHttpRequest mutatedRequest = requestBuilder.build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Checks whether the request path matches a configured public route.
     *
     * @param path the incoming request path
     * @return true if the path does not require JWT authentication
     */
    private boolean isPublicRoute(String path) {
        return securityProperties.publicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Returns a 401 Unauthorized response and stops gateway request processing.
     *
     * @param exchange the current HTTP request and response exchange
     * @return a Mono that completes after the unauthorized response is sent
     */
    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Defines this filter's execution order in the gateway filter chain.
     *
     * @return the filter execution order
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
