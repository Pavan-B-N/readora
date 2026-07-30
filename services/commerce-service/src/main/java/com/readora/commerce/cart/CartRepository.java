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

// Cart is stored as one JSON-encoded list under a single Redis key per user — Redis is the system of record, no Postgres table backs it; a single key holding the whole list was simpler than a true hash-per-item, and the 30-day idle TTL still applies the same effect.
@Repository
public class CartRepository {

    private static final Duration TTL = Duration.ofDays(30);
    private static final TypeReference<List<CartItemData>> ITEM_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Wires the Redis template and JSON mapper used to (de)serialize cart items.
    public CartRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // Reads the cart's item list for a user, or an empty list if none/unparseable.
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

    // Serializes and stores the full item list for a user, refreshing the TTL.
    public void saveItems(UUID userId, List<CartItemData> items) {
        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(key(userId), json, TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cart", e);
        }
    }

    // Deletes the cart key for a user.
    public void clear(UUID userId) {
        redisTemplate.delete(key(userId));
    }

    // Builds the Redis key for a user's cart.
    private String key(UUID userId) {
        return "cart:" + userId;
    }
}
