package com.readora.delivery.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GatewaySecretFilterTest {

    private static final String SECRET = "expected-shared-secret";

    private GatewaySecretFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewaySecretFilter(SECRET, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @Test
    void correctSecret_letsRequestThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewaySecretFilter.GATEWAY_SECRET_HEADER, SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void wrongSecret_rejectsWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(GatewaySecretFilter.GATEWAY_SECRET_HEADER, "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void missingSecret_rejectsWith403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void getOrder_runsBeforeEveryOtherFilter() {
        assertThat(filter.getOrder()).isEqualTo(-30);
    }
}
