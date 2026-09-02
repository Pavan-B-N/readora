package com.readora.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.gateway.config.RateLimitProperties;
import com.readora.gateway.config.RateLimitProperties.RateLimitRule;
import com.readora.sharedcore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingGlobalFilterTest {

    @Mock private RedisRateLimiterService rateLimiterService;
    @Mock private JwtService jwtService;

    private RateLimitingGlobalFilter filter;
    private final RateLimitProperties properties = new RateLimitProperties(
            new RateLimitRule(200, 60), Map.of("auth-service", new RateLimitRule(20, 60)));

    @BeforeEach
    void setUp() {
        filter = new RateLimitingGlobalFilter(rateLimiterService, properties, new ObjectMapper(), jwtService);
    }

    @Test
    void filter_underLimit_forwardsRequest() {
        when(rateLimiterService.isAllowed(anyString(), any())).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/books").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_overLimit_rejectsWith429AndRetryAfterHeader() {
        when(rateLimiterService.isAllowed(anyString(), any())).thenReturn(Mono.just(false));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/books").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_authenticatedCaller_keysByUserIdNotIp() {
        UUID userId = UUID.randomUUID();
        when(jwtService.extractUserId("good-token")).thenReturn(Optional.of(userId));
        when(rateLimiterService.isAllowed(anyString(), any())).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/cart").header("Authorization", "Bearer good-token").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(rateLimiterService).isAllowed(keyCaptor.capture(), any());
        assertThat(keyCaptor.getValue()).contains(userId.toString());
    }

    @Test
    void filter_noRouteMatched_usesDefaultRule() {
        when(rateLimiterService.isAllowed(anyString(), any())).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unmatched").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        org.mockito.ArgumentCaptor<RateLimitRule> ruleCaptor = org.mockito.ArgumentCaptor.forClass(RateLimitRule.class);
        verify(rateLimiterService).isAllowed(anyString(), ruleCaptor.capture());
        assertThat(ruleCaptor.getValue().limit()).isEqualTo(200);
    }

    @Test
    void getOrder_runsAfterAuthenticationFilters() {
        assertThat(filter.getOrder()).isEqualTo(0);
    }
}
