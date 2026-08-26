package com.readora.notification.filter;

import com.readora.notification.security.CurrentUserContext;
import com.readora.notification.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads and validates the Authorization: Bearer header for REST calls (routed through
 * api-gateway, unlike the STOMP WebSocket endpoint which validates the token itself on CONNECT).
 * Populates CurrentUserContext; UserContextFilter (which runs after this one) enforces which
 * routes actually require an authenticated caller.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<UUID> userId = jwtService.validate(token);

            if (userId.isPresent()) {
                try {
                    CurrentUserContext.set(userId.get());
                    filterChain.doFilter(request, response);
                } finally {
                    CurrentUserContext.clear();
                }
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
