package com.readora.payment.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.payment.config.SecurityProperties;
import com.readora.payment.dto.ErrorResponse;
import com.readora.payment.security.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Deny-by-default, same shape as api-gateway's JwtAuthenticationGlobalFilter: routes not in
 * app.security.public-routes require X-User-Id to be present. Trusts the header rather than
 * validating a JWT itself, since GatewaySecretFilter already proved the request came from
 * inside the trusted network (gateway or another internal service).
 */
@Component
public class UserContextFilter extends OncePerRequestFilter implements Ordered {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public UserContextFilter(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicRoute(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            reject(request, response);
            return;
        }

        try {
            CurrentUserContext.set(UUID.fromString(userIdHeader));
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private boolean isPublicRoute(String path) {
        return securityProperties.getPublicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                "UNAUTHENTICATED",
                "This endpoint requires an authenticated caller.",
                HttpStatus.UNAUTHORIZED.value(),
                request.getRequestURI(),
                null,
                Instant.now()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
