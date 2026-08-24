package com.readora.ai.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.ai.config.SecurityProperties;
import com.readora.ai.dto.ErrorResponse;
import com.readora.ai.security.CurrentUserContext;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Deny-by-default for authentication, same as before. Additionally gates /api/v1/admin/** on
 * the ADMIN role — X-User-Roles is only trustworthy here because GatewaySecretFilter already
 * proved this request came through the gateway, which is the only thing that sets that header.
 */
@Component
public class UserContextFilter extends OncePerRequestFilter implements Ordered {

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

        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            rejectUnauthenticated(request, response);
            return;
        }

        List<String> roles = parseRoles(request.getHeader("X-User-Roles"));

        try {
            CurrentUserContext.set(UUID.fromString(userIdHeader), roles);

            if (path.startsWith(ADMIN_PATH_PREFIX) && !CurrentUserContext.hasRole(ADMIN_ROLE)) {
                rejectForbidden(request, response);
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private List<String> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        return Arrays.stream(header.split(",")).map(String::trim).toList();
    }

    private boolean isPublicRoute(String path) {
        return securityProperties.publicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "This endpoint requires an authenticated caller.");
    }

    private void rejectForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "This endpoint requires the ADMIN role.");
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
