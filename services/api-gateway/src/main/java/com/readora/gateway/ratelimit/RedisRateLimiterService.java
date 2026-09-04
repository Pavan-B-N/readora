package com.readora.gateway.ratelimit;

import com.readora.gateway.config.RateLimitProperties.RateLimitRule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Token-bucket rate limiter backed by Bucket4j's distributed Redis state.
 * Buckets refill continuously rather than resetting all-at-once, so callers can't burst past the limit at a window boundary.
 */
@Component
public class RedisRateLimiterService {

    private final AsyncProxyManager<String> proxyManager;

    public RedisRateLimiterService(AsyncProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    /** Creates the bucket with the rule's capacity/refill rate on first use. */
    public Mono<Boolean> isAllowed(String key, RateLimitRule rule) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rule.limit())
                        .refillIntervally(rule.limit(), Duration.ofSeconds(rule.windowSeconds()))
                        .build())
                .build();

        return Mono.fromFuture(
                proxyManager.builder()
                        .build(key, () -> java.util.concurrent.CompletableFuture.completedFuture(configuration))
                        .tryConsume(1)
        );
    }
}
