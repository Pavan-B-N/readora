package com.readora.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads the X-Correlation-Id the gateway attached (falls back to generating one, for requests
 * that reach this service directly, e.g. in local testing without the gateway running) and puts
 * it in MDC so it's available to GlobalExceptionHandler and to log output for the life of the
 * request.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** The header name the gateway sets, and this filter echoes back on the response. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /** The MDC key the correlation id is stored under for the life of the request. */
    public static final String MDC_KEY = "correlationId";

    /**
     * Resolves the request's correlation id (from the header, or freshly generated), stores it
     * in MDC for the duration of the request, and echoes it back on the response.
     *
     * @param request     the incoming request
     * @param response    the outgoing response
     * @param filterChain the remaining filter chain to continue to
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
