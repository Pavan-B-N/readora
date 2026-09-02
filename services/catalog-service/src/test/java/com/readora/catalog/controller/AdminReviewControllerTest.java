package com.readora.catalog.controller;

import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.catalog.exception.ReviewNotFoundException;
import com.readora.catalog.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminReviewControllerTest {

    @Mock private ReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void delete_found_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/reviews/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_mapsTo404() throws Exception {
        org.mockito.Mockito.doThrow(new ReviewNotFoundException()).when(reviewService).deleteReviewAsAdmin(any());

        mockMvc.perform(delete("/api/v1/admin/reviews/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
