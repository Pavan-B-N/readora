package com.readora.gateway.ratelimit;

import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires a dedicated Lettuce connection for Bucket4j's distributed bucket state.
 * Kept separate from Spring Data Redis's own client since Bucket4j manages this connection directly with its own byte codec.
 */
@Configuration
public class Bucket4jRedisConfig {

    /**
     * Creates a dedicated Lettuce client for Bucket4j, pointed at the same Redis instance the
     * rest of the gateway uses.
     *
     * @param host the Redis host, from spring.data.redis.host
     * @param port the Redis port, from spring.data.redis.port
     * @return a Lettuce client connected to that Redis instance
     */
    @Bean
    public RedisClient bucket4jRedisClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port
    ) {
        return RedisClient.create(RedisURI.Builder.redis(host, port).build());
    }

    /**
     * Opens a connection using a String-key/byte-array-value codec — the format Bucket4j's
     * Redis integration requires to serialize bucket state.
     *
     * @param bucket4jRedisClient the Lettuce client to connect with
     * @return a connection using the codec Bucket4j expects
     */
    @Bean
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient bucket4jRedisClient) {
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        return bucket4jRedisClient.connect(codec);
    }

    /**
     * Builds the proxy manager RedisRateLimiterService uses to look up and create buckets by
     * key, backed by the Redis connection above.
     *
     * @param bucket4jRedisConnection the codec-configured Redis connection to back buckets with
     * @return an async proxy manager for creating/consuming buckets keyed by rate-limit key
     */
    @Bean
    public AsyncProxyManager<String> bucketProxyManager(StatefulRedisConnection<String, byte[]> bucket4jRedisConnection) {
        return LettuceBasedProxyManager.builderFor(bucket4jRedisConnection)
                .build()
                .asAsync();
    }
}
