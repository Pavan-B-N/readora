package com.readora.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.auth.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Rejects any request that didn't come through api-gateway. The gateway stamps every proxied
 * request with this shared secret (AddRequestHeader filter in its application.yml); without this
 * check, hitting auth-service directly on its own port would bypass the gateway's rate limiting
 * and JWT enforcement entirely. Runs first in the chain — before correlation-id assignment or
 * JWT handling — so illegitimate requests are rejected before any other work happens, and before
 * anything downstream trusts any of their other headers.
 */
@Component
public class GatewaySecretFilter extends OncePerRequestFilter {

    /** The header the gateway (or another internal service) must present the shared secret in. */
    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final byte[] expectedSecret;
    private final ObjectMapper objectMapper;

    /**
     * @param expectedSecret the shared secret this service expects on every request
     * @param objectMapper   used to serialize the rejection response body
     */
    public GatewaySecretFilter(@Value("${app.gateway.secret}") String expectedSecret, ObjectMapper objectMapper) {
        this.expectedSecret = expectedSecret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    /**
     * Compares the presented X-Gateway-Secret header (in constant time, to avoid a timing
     * side-channel) against the expected secret, rejecting with 403 if it's missing or wrong.
     *
     * @param request     the incoming request
     * @param response    the outgoing response
     * @param filterChain the remaining filter chain to continue to if the secret matches
     */
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

    /**
     * Writes a 403 DIRECT_ACCESS_FORBIDDEN error body and stops the filter chain.
     *
     * @param request  the incoming request, used to read the request path
     * @param response the outgoing response to write the rejection body to
     */
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
}
