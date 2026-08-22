package com.readora.gateway.ratelimit;

import com.readora.gateway.config.RateLimitProperties.RateLimitRule;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Atomic fixed-window counter in Redis: INCR the key, and only on the very first hit in a
 * window (count == 1) set its TTL — so the window doesn't reset on every request. Both
 * operations run as one Lua script so concurrent requests can't race past the limit.
 */
@Component
public class RedisRateLimiterService {

    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of(
            "local current = redis.call('INCR', KEYS[1]) " +
                    "if tonumber(current) == 1 then " +
                    "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                    "end " +
                    "return current",
            Long.class
    );

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisRateLimiterService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isAllowed(String key, RateLimitRule rule) {
        return redisTemplate.execute(
                        INCREMENT_SCRIPT,
                        List.of(key),
                        List.of(String.valueOf(rule.windowSeconds()))
                )
                .next()
                .map(count -> count <= rule.limit());
    }
}
