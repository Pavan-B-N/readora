package com.readora.gateway.filter;

import com.readora.gateway.config.SecurityProperties;
import com.readora.sharedcore.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

    private final JwtService jwtService;
    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationGlobalFilter(JwtService jwtService, SecurityProperties securityProperties) {
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    /** Only validates the token — the original Authorization header is forwarded to downstream services untouched. */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("Rejecting {} {} — missing or malformed Authorization header", exchange.getRequest().getMethod(), path);
            return reject(exchange);
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            log.debug("Rejecting {} {} — invalid or expired token", exchange.getRequest().getMethod(), path);
            return reject(exchange);
        }

        return chain.filter(exchange);
    }

    /** Always public, regardless of the configured public-routes list — kubelet's probe relies on it. */
    private static final String ACTUATOR_HEALTH_PREFIX = "/actuator/health";

    private boolean isPublicRoute(String path) {
        return path.startsWith(ACTUATOR_HEALTH_PREFIX)
                || securityProperties.publicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
