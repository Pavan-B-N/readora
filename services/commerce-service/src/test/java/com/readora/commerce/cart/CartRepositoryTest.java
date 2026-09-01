package com.readora.commerce.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.entity.DeliveryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartRepositoryTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private CartRepository cartRepository;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cartRepository = new CartRepository(redisTemplate, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void getItems_noExistingKey_returnsEmptyList() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cart:" + userId)).thenReturn(null);

        assertThat(cartRepository.getItems(userId)).isEmpty();
    }

    @Test
    void getItems_malformedJson_degradesToEmptyList() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cart:" + userId)).thenReturn("not valid json");

        assertThat(cartRepository.getItems(userId)).isEmpty();
    }

    @Test
    void saveItems_thenGetItems_roundTrips() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<CartItemData> items = List.of(
                new CartItemData(UUID.randomUUID(), "Title", 2, new BigDecimal("100.00"), DeliveryType.PHYSICAL, Instant.now()));

        cartRepository.saveItems(userId, items);

        verify(valueOperations).set(eq("cart:" + userId), any(), eq(Duration.ofDays(30)));
    }

    @Test
    void clear_deletesTheKey() {
        cartRepository.clear(userId);

        verify(redisTemplate).delete("cart:" + userId);
    }
}
