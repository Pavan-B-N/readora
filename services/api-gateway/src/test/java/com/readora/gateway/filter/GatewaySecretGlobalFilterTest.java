package com.readora.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewaySecretGlobalFilterTest {

    private final GatewaySecretGlobalFilter filter = new GatewaySecretGlobalFilter("shared-secret");

    @Test
    void filter_addsGatewaySecretHeaderToForwardedRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/books").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        org.mockito.ArgumentCaptor<ServerWebExchange> captor = org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Gateway-Secret")).isEqualTo("shared-secret");
    }

    @Test
    void getOrder_runsFirst() {
        assertThat(filter.getOrder()).isEqualTo(-3);
    }
}
