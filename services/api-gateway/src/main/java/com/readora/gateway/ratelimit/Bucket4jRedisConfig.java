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

    @Bean
    public RedisClient bucket4jRedisClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port
    ) {
        return RedisClient.create(RedisURI.Builder.redis(host, port).build());
    }

    /** String-key/byte-array-value codec — the format Bucket4j's Redis integration requires. */
    @Bean
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(RedisClient bucket4jRedisClient) {
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        return bucket4jRedisClient.connect(codec);
    }

    @Bean
    public AsyncProxyManager<String> bucketProxyManager(StatefulRedisConnection<String, byte[]> bucket4jRedisConnection) {
        return LettuceBasedProxyManager.builderFor(bucket4jRedisConnection)
                .build()
                .asAsync();
    }
}
