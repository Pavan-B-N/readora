package com.readora.payment.filter;

import com.readora.payment.security.CurrentUserContext;
import com.readora.payment.security.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void validToken_populatesContextForTheDurationOfTheChainThenClears() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        UUID userId = UUID.randomUUID();
        when(jwtService.extractUserId("good-token")).thenReturn(Optional.of(userId));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            assertThat(CurrentUserContext.get()).contains(userId);
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(CurrentUserContext.get()).isEmpty();
    }

    @Test
    void invalidToken_leavesContextEmptyButStillContinuesChain() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        when(jwtService.extractUserId("bad-token")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(CurrentUserContext.get()).isEmpty();
        verify(chain).doFilter(request, response);
    }

    @Test
    void noAuthorizationHeader_leavesContextEmpty() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(CurrentUserContext.get()).isEmpty();
        verify(chain).doFilter(request, response);
    }

    @Test
    void getOrder_matchesGatewaySecretFilterOrdering() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        assertThat(filter.getOrder()).isEqualTo(-20);
    }
}
