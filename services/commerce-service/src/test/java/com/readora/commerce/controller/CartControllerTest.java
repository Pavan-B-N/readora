package com.readora.commerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.commerce.dto.AddCartItemRequest;
import com.readora.commerce.dto.CartResponse;
import com.readora.commerce.dto.CartSummaryResponse;
import com.readora.commerce.dto.SetCartItemRequest;
import com.readora.commerce.entity.DeliveryType;
import com.readora.commerce.exception.GlobalExceptionHandler;
import com.readora.commerce.exception.InsufficientStockException;
import com.readora.commerce.security.CurrentUserContext;
import com.readora.commerce.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock private CartService cartService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        CurrentUserContext.set(UUID.randomUUID(), List.of("CUSTOMER"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void getCart_delegatesToService() throws Exception {
        when(cartService.getCart(any())).thenReturn(new CartResponse(List.of(), BigDecimal.ZERO, "INR", 0, false));

        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isOk());
    }

    @Test
    void addItem_valid_returns200() throws Exception {
        when(cartService.addItem(any(), any())).thenReturn(new CartSummaryResponse(1, new BigDecimal("100.00"), "INR"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AddCartItemRequest(UUID.randomUUID(), 1, DeliveryType.PHYSICAL, UUID.randomUUID()))))
                .andExpect(status().isOk());
    }

    @Test
    void addItem_insufficientStock_mapsTo409() throws Exception {
        when(cartService.addItem(any(), any())).thenThrow(new InsufficientStockException("out of stock"));

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AddCartItemRequest(UUID.randomUUID(), 1, DeliveryType.PHYSICAL, UUID.randomUUID()))))
                .andExpect(status().isConflict());
    }

    @Test
    void addItem_qtyBelowOne_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new AddCartItemRequest(UUID.randomUUID(), 0, DeliveryType.PHYSICAL, UUID.randomUUID()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setItemQty_valid_returns200() throws Exception {
        when(cartService.setItemQty(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new CartSummaryResponse(2, new BigDecimal("200.00"), "INR"));

        mockMvc.perform(put("/api/v1/cart/items/" + UUID.randomUUID() + "/PHYSICAL")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SetCartItemRequest(2))))
                .andExpect(status().isOk());
    }
}
