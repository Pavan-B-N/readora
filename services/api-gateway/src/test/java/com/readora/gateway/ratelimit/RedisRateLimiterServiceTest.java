package com.readora.gateway.ratelimit;

import com.readora.gateway.config.RateLimitProperties.RateLimitRule;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteAsyncBucketBuilder;
import io.github.bucket4j.distributed.AsyncBucketProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterServiceTest {

    @Mock private AsyncProxyManager<String> proxyManager;
    @Mock private RemoteAsyncBucketBuilder<String> bucketBuilder;
    @Mock private AsyncBucketProxy bucketProxy;

    private RedisRateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RedisRateLimiterService(proxyManager);
    }

    @Test
    void isAllowed_tokenAvailable_returnsTrue() {
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(java.util.function.Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsume(1)).thenReturn(CompletableFuture.completedFuture(true));

        Boolean allowed = service.isAllowed("ratelimit:default:1.2.3.4", new RateLimitRule(200, 60)).block();

        assertThat(allowed).isTrue();
    }

    @Test
    void isAllowed_noTokensLeft_returnsFalse() {
        when(proxyManager.builder()).thenReturn(bucketBuilder);
        when(bucketBuilder.build(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(java.util.function.Supplier.class))).thenReturn(bucketProxy);
        when(bucketProxy.tryConsume(1)).thenReturn(CompletableFuture.completedFuture(false));

        Boolean allowed = service.isAllowed("ratelimit:default:1.2.3.4", new RateLimitRule(200, 60)).block();

        assertThat(allowed).isFalse();
    }
}
