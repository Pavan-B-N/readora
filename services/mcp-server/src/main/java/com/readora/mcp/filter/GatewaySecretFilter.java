package com.readora.mcp.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

/**
 * Rejects any request that doesn't present the shared X-Gateway-Secret header, same convention
 * as every other service — the only expected caller is ai-service's MCP client, not an end user,
 * so there is no separate public-routes list here.
 */
@Component
public class GatewaySecretFilter extends OncePerRequestFilter implements Ordered {

    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final byte[] expectedSecret;
    private final ObjectMapper objectMapper;

    public GatewaySecretFilter(@Value("${app.gateway.secret}") String expectedSecret, ObjectMapper objectMapper) {
        this.expectedSecret = expectedSecret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
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

        Map<String, Object> body = Map.of(
                "code", "DIRECT_ACCESS_FORBIDDEN",
                "message", "This service only accepts requests from ai-service.",
                "path", request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    public int getOrder() {
        return -30;
    }
}
