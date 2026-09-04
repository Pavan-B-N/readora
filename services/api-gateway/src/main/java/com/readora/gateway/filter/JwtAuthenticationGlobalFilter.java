package com.readora.gateway.filter;

import com.readora.gateway.config.SecurityProperties;
import com.readora.sharedcore.security.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
     * Enforces JWT authentication for protected gateway routes. Only validates the token — the
     * original Authorization header is forwarded to downstream services untouched, and each
     * service extracts what it needs from the JWT itself.
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

        if (!jwtService.isValid(token)) {
            return reject(exchange);
        }

        return chain.filter(exchange);
    }

    /**
     * Checks whether the request path matches a configured public route.
     *
     * @param path the incoming request path
     * @return true if the path does not require JWT authentication
     */
    /** Always public, regardless of the configured public-routes list — kubelet's probe relies on it. */
    private static final String ACTUATOR_HEALTH_PREFIX = "/actuator/health";

    private boolean isPublicRoute(String path) {
        return path.startsWith(ACTUATOR_HEALTH_PREFIX)
                || securityProperties.publicRoutes().stream()
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
