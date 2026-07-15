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

// Deny-by-default for authentication, plus config-driven role gates (see SecurityProperties); identity comes from CurrentUserContext, populated earlier by JwtAuthenticationFilter.
@Component
public class UserContextFilter extends OncePerRequestFilter implements Ordered {

    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Wires in this service's own app.security.* config.
    public UserContextFilter(SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    // Lets public routes through unchecked, then enforces authentication and any matching role gate.
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

    /** Always public, in every service, regardless of each one's own public-routes config — kubelet's probes rely on it. */
    private static final String ACTUATOR_HEALTH_PREFIX = "/actuator/health";

    /** API docs are read-only documentation, not data — always public in every service, same reasoning as health. */
    private static final String[] OPEN_DOC_PREFIXES = {
            "/swagger-ui",
            "/v3/api-docs"
    };

    // True for the health prefix, API docs, or any path matching this service's own public-routes config.
    private boolean isPublicRoute(String path) {
        return path.startsWith(ACTUATOR_HEALTH_PREFIX)
                || isOpenDocRequest(path)
                || securityProperties.publicRoutes().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // True for springdoc's UI page, its static assets, and the raw OpenAPI JSON/YAML it fetches.
    private boolean isOpenDocRequest(String path) {
        for (String prefix : OPEN_DOC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // True for a GET matching this service's own public-get-routes config (write methods still require auth).
    private boolean isPublicGet(HttpServletRequest request, String path) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && securityProperties.publicGetRoutes().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // Writes the standard 401 UNAUTHENTICATED body.
    private void rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeError(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "This endpoint requires an authenticated caller.");
    }

    // Writes the standard 403 FORBIDDEN body, naming the missing role.
    private void rejectForbidden(HttpServletRequest request, HttpServletResponse response, String requiredRole) throws IOException {
        writeError(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "This endpoint requires the " + requiredRole + " role.");
    }

    // Shared JSON-error-body writer both reject methods delegate to.
    private void writeError(
            HttpServletRequest request, HttpServletResponse response, HttpStatus status, String code, String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(code, message, status.value(), request.getRequestURI(), null, Instant.now());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    // Runs last among the security filters, after identity (JwtAuthenticationFilter) is already populated.
    @Override
    public int getOrder() {
        return -10;
    }
}
