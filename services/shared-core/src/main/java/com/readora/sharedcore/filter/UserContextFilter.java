package com.readora.sharedcore.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.sharedcore.config.SecurityProperties;
import com.readora.sharedcore.dto.ErrorResponse;
import com.readora.sharedcore.security.CurrentUserContext;
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
 * Deny-by-default for authentication, plus config-driven role gates (see SecurityProperties) —
 * e.g. /api/v1/admin/** requiring ADMIN, or /api/v1/delivery/** requiring DELIVERY_AGENT.
 * Identity and roles come from CurrentUserContext, populated by JwtAuthenticationFilter after
 * validating the caller's JWT — this filter itself never reads or trusts any headers.
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

        if (isPublicRoute(path) || isPublicGet(request, path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (CurrentUserContext.get().isEmpty()) {
            rejectUnauthenticated(request, response);
            return;
        }

        for (SecurityProperties.RoleGate gate : securityProperties.roleGates()) {
            if (path.startsWith(gate.pathPrefix()) && !CurrentUserContext.hasRole(gate.role())) {
                rejectForbidden(request, response, gate.role());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicRoute(String path) {
        return securityProperties.publicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isPublicGet(HttpServletRequest request, String path) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && securityProperties.publicGetRoutes().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
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
