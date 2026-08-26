package com.readora.catalog.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.config.SecurityProperties;
import com.readora.catalog.dto.ErrorResponse;
import com.readora.catalog.security.CurrentUserContext;
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

/**
 * Deny-by-default for authentication, same as before. Additionally gates /api/v1/admin/** on
 * the ADMIN role. Identity and roles come from CurrentUserContext, populated by
 * JwtAuthenticationFilter after validating the caller's JWT — this filter itself no longer reads
 * or trusts any headers.
 */
@Component
public class UserContextFilter extends OncePerRequestFilter implements Ordered {

    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";
    private static final String ADMIN_ROLE = "ADMIN";

    /**
     * Reading reviews is public, posting one isn't — the shared publicRoutes list has no
     * per-method concept (every other public route is GET-only anyway, so this hasn't mattered
     * until now), so this one path gets a narrow, explicit GET-only exemption instead of widening
     * that config format for a single case.
     */
    private static final String REVIEWS_PATH = "/api/v1/books/*/reviews";

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

        if (isPublicRoute(path) || isPublicGet(request, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (CurrentUserContext.get().isEmpty()) {
            rejectUnauthenticated(request, response);
            return;
        }

        if (path.startsWith(ADMIN_PATH_PREFIX) && !CurrentUserContext.hasRole(ADMIN_ROLE)) {
            rejectForbidden(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicRoute(String path) {
        return securityProperties.publicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isPublicGet(HttpServletRequest request, String path) {
        return "GET".equalsIgnoreCase(request.getMethod()) && pathMatcher.match(REVIEWS_PATH, path);
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
