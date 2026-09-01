package com.readora.commerce.controller;

import com.readora.commerce.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalOrderControllerTest {

    @Mock private OrderItemRepository orderItemRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalOrderController(orderItemRepository)).build();
    }

    @Test
    void purchasedBookIds_delegatesToRepository() throws Exception {
        when(orderItemRepository.findDistinctBookIdsByUserId(any())).thenReturn(List.of(UUID.randomUUID()));

        mockMvc.perform(get("/internal/orders/purchased-book-ids").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void recentItems_delegatesToRepository() throws Exception {
        when(orderItemRepository.findRecentByUserId(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/internal/orders/recent-items").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }
}
