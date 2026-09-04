package com.readora.sharedcore.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.readora.sharedcore.config.SecurityProperties;
import com.readora.sharedcore.security.CurrentUserContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserContextFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Mirrors delivery-agent-service's real shape: two DELIVERY_AGENT-gated prefixes plus one
    // ADMIN-gated prefix, to prove multiple simultaneous role gates work, not just a single one.
    private final UserContextFilter filter = new UserContextFilter(
            new SecurityProperties(
                    List.of("/internal/**", "/api/v1/books", "/api/v1/books/*"),
                    List.of("/api/v1/books/*/reviews"),
                    List.of(
                            new SecurityProperties.RoleGate("/api/v1/delivery/", "DELIVERY_AGENT"),
                            new SecurityProperties.RoleGate("/api/v1/returns/", "DELIVERY_AGENT"),
                            new SecurityProperties.RoleGate("/api/v1/admin/", "ADMIN")
                    )
            ),
            objectMapper
    );

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void publicRoute_letsRequestThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void publicGetRoute_getWithoutAuth_isAllowed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books/abc/reviews");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void publicGetRoute_postWithoutAuth_isRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/books/abc/reviews");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void protectedRoute_noAuthenticatedUser_rejectsWith401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cart");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void adminRoute_authenticatedWithoutAdminRole_rejectsWith403() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("ADMIN");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void adminRoute_authenticatedWithAdminRole_letsRequestThrough() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void deliveryRoute_authenticatedWithoutDeliveryAgentRole_rejectsWith403() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("ADMIN"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/delivery/queue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("DELIVERY_AGENT");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void returnsRoute_secondGateWithSameRole_alsoEnforced() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/returns/queue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void deliveryRoute_authenticatedWithDeliveryAgentRole_letsRequestThrough() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("DELIVERY_AGENT"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/delivery/queue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void protectedRouteWithNoRoleGate_authenticatedCaller_letsRequestThrough() throws Exception {
        // A route not matched by any RoleGate — plain-auth-only, same as payment-service/notification-service.
        CurrentUserContext.set(UUID.randomUUID());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/xyz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void getOrder_runsAfterJwtAuthenticationFilter() {
        assertThat(filter.getOrder()).isEqualTo(-10);
    }

    @Test
    void actuatorHealthPath_unauthenticated_isPublicRegardlessOfServiceConfig() throws Exception {
        // Not in this filter's own publicRoutes list — proves the exemption is unconditional, not config-driven.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/liveness");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
