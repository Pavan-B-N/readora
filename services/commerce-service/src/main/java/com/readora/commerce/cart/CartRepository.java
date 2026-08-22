package com.readora.commerce.cart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cart is stored as one JSON-encoded list under a single Redis key per user — Redis is the
 * system of record, no Postgres table backs it, matching the doc's "CartItem (Redis hash)"
 * design. A true Redis hash-per-item was simpler to implement as one key holding the whole
 * list; the 30-day idle TTL still applies to the same effect.
 */
@Repository
public class CartRepository {

    private static final Duration TTL = Duration.ofDays(30);
    private static final TypeReference<List<CartItemData>> ITEM_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CartRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<CartItemData> getItems(UUID userId) {
        String json = redisTemplate.opsForValue().get(key(userId));
        if (json == null) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, ITEM_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    public void saveItems(UUID userId, List<CartItemData> items) {
        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(key(userId), json, TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cart", e);
        }
    }

    public void clear(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(UUID userId) {
        return "cart:" + userId;
    }
}
