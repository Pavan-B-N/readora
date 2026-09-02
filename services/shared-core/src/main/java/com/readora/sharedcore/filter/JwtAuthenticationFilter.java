package com.readora.sharedcore.filter;

import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.sharedcore.security.JwtService;
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
 * Reads and validates the Authorization: Bearer header, if present, and populates
 * CurrentUserContext with the caller's identity — actual authorization (which routes require an
 * authenticated caller, and any role gates) is enforced separately by UserContextFilter, which
 * runs after this one.
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
            Optional<UUID> userId = jwtService.extractUserId(token);

            if (userId.isPresent()) {
                try {
                    CurrentUserContext.set(
                            userId.get(), jwtService.extractRoles(token), jwtService.extractEmail(token).orElse(null)
                    );
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
