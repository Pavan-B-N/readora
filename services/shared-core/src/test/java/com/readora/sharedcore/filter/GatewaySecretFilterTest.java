package com.readora.sharedcore.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GatewaySecretFilterTest {

    private final GatewaySecretFilter filter = new GatewaySecretFilter(
            "the-real-secret", new ObjectMapper().registerModule(new JavaTimeModule())
    );

    @Test
    void matchingSecret_letsRequestThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewaySecretFilter.GATEWAY_SECRET_HEADER, "the-real-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void wrongSecret_rejectsWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewaySecretFilter.GATEWAY_SECRET_HEADER, "guessed-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("DIRECT_ACCESS_FORBIDDEN");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void missingHeader_rejectsWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void getOrder_isMinusThirty() {
        assertThat(filter.getOrder()).isEqualTo(-30);
    }

    @Test
    void actuatorHealthPath_noHeaderAtAll_stillLetsRequestThrough() throws Exception {
        // kubelet's probe hits this directly, never through the gateway — no header to check.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/readiness");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
