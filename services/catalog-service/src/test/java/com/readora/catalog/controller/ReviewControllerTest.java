package com.readora.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.dto.ReviewResponse;
import com.readora.catalog.dto.UpsertReviewRequest;
import com.readora.catalog.security.CurrentUserContext;
import com.readora.catalog.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock private ReviewService reviewService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService)).build();
        CurrentUserContext.set(UUID.randomUUID(), List.of("CUSTOMER"));
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void list_delegatesToService() throws Exception {
        when(reviewService.getReviews(any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID() + "/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    void upsert_valid_returns200() throws Exception {
        when(reviewService.upsertReview(any(), any(), any())).thenReturn(
                new ReviewResponse(UUID.randomUUID(), UUID.randomUUID(), "Reader", 5, "Great", false, Instant.now()));

        mockMvc.perform(post("/api/v1/books/" + UUID.randomUUID() + "/reviews")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpsertReviewRequest(5, "Great"))))
                .andExpect(status().isOk());
    }

    @Test
    void upsert_ratingOutOfRange_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/books/" + UUID.randomUUID() + "/reviews")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpsertReviewRequest(6, "Great"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteOwn_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/books/" + UUID.randomUUID() + "/reviews/me"))
                .andExpect(status().isNoContent());
    }
}
