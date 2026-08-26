package com.readora.notification.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.notification.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Component
public class GatewaySecretFilter extends OncePerRequestFilter implements Ordered {

    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final byte[] expectedSecret;
    private final ObjectMapper objectMapper;

    public GatewaySecretFilter(@Value("${app.gateway.secret}") String expectedSecret, ObjectMapper objectMapper) {
        this.expectedSecret = expectedSecret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    /**
     * The STOMP/SockJS endpoint is deliberately reachable directly by the browser (see
     * WebSocketConfig's allowedOriginPatterns) — unlike every other route here, it was never
     * meant to be gateway-only, so it can't carry a secret only server-to-server callers know.
     * Its own auth happens at the STOMP CONNECT frame via StompAuthChannelInterceptor instead.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/ws");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String presented = request.getHeader(GATEWAY_SECRET_HEADER);

        if (presented == null
                || !MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expectedSecret)) {
            reject(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                "DIRECT_ACCESS_FORBIDDEN",
                "This service only accepts requests routed through the API gateway.",
                HttpStatus.FORBIDDEN.value(),
                request.getRequestURI(),
                null,
                Instant.now()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    public int getOrder() {
        return -30;
    }
}
