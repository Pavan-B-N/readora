package com.readora.notification.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.readora.notification.config.SecurityProperties;
import com.readora.notification.security.CurrentUserContext;
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
            new SecurityProperties(List.of("/ws/**")),
            new ObjectMapper().registerModule(new JavaTimeModule())
    );

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void publicRoute_letsRequestThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws/info");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void protectedRoute_noAuthenticatedUser_rejectsWith401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void protectedRoute_authenticatedUser_letsRequestThrough() throws Exception {
        CurrentUserContext.set(UUID.randomUUID());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
