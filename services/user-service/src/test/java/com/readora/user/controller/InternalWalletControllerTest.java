package com.readora.user.controller;

import com.readora.user.dto.WalletBalanceResponse;
import com.readora.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalWalletControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalWalletController(userService)).build();
    }

    @Test
    void getBalance_returnsCurrentBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getBalance(userId)).thenReturn(new WalletBalanceResponse(new BigDecimal("123.45"), "INR"));

        mockMvc.perform(get("/internal/wallet/" + userId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(123.45))
                .andExpect(jsonPath("$.currency").value("INR"));
    }
}
