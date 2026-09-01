package com.readora.delivery.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.readora.delivery.config.SecurityProperties;
import com.readora.delivery.security.CurrentUserContext;
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

    private final UserContextFilter filter = new UserContextFilter(
            new SecurityProperties(List.of("/internal/**")),
            new ObjectMapper().registerModule(new JavaTimeModule())
    );

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void publicRoute_letsRequestThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/orders/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void protectedRoute_noAuthenticatedUser_rejectsWith401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/delivery/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void deliveryRoute_authenticatedWithoutDeliveryAgentRole_rejectsWith403() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("CUSTOMER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/delivery/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("DELIVERY_AGENT");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void deliveryRoute_authenticatedWithDeliveryAgentRole_letsRequestThrough() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("DELIVERY_AGENT"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/delivery/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void returnsRoute_requiresDeliveryAgentRoleToo() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("DELIVERY_AGENT"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/returns/assignments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void adminRoute_authenticatedWithoutAdminRole_rejectsWith403() throws Exception {
        CurrentUserContext.set(UUID.randomUUID(), List.of("DELIVERY_AGENT"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/delivery/agents");
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/delivery/agents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
