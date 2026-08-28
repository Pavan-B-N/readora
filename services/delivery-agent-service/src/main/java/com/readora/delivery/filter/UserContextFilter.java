package com.readora.delivery.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.delivery.config.SecurityProperties;
import com.readora.delivery.dto.ErrorResponse;
import com.readora.delivery.security.CurrentUserContext;
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
import java.util.List;

/**
 * Deny-by-default for authentication. Additionally gates /api/v1/delivery/** and /api/v1/returns/**
 * on the DELIVERY_AGENT role, and /api/v1/admin/** on the ADMIN role — same "prefix check in this
 * filter" convention catalog-service uses. Identity and roles come from CurrentUserContext,
 * populated by JwtAuthenticationFilter after validating the caller's JWT.
 */
@Component
public class UserContextFilter extends OncePerRequestFilter implements Ordered {

    private static final List<String> DELIVERY_AGENT_PATH_PREFIXES = List.of("/api/v1/delivery/", "/api/v1/returns/");
    private static final String DELIVERY_AGENT_ROLE = "DELIVERY_AGENT";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";
    private static final String ADMIN_ROLE = "ADMIN";

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

        if (CurrentUserContext.get().isEmpty()) {
            rejectUnauthenticated(request, response);
            return;
        }

        boolean requiresDeliveryAgentRole = DELIVERY_AGENT_PATH_PREFIXES.stream().anyMatch(path::startsWith);
        if (requiresDeliveryAgentRole && !CurrentUserContext.hasRole(DELIVERY_AGENT_ROLE)) {
            rejectForbidden(request, response, DELIVERY_AGENT_ROLE);
            return;
        }

        if (path.startsWith(ADMIN_PATH_PREFIX) && !CurrentUserContext.hasRole(ADMIN_ROLE)) {
            rejectForbidden(request, response, ADMIN_ROLE);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicRoute(String path) {
        return securityProperties.publicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "This endpoint requires an authenticated caller.");
    }

    private void rejectForbidden(HttpServletRequest request, HttpServletResponse response, String requiredRole) throws IOException {
        writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "This endpoint requires the " + requiredRole + " role.");
    }

    private void writeError(
            HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(code, message, status.value(), request.getRequestURI(), null, Instant.now());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
