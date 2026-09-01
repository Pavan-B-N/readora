package com.readora.catalog.controller;

import com.readora.catalog.dto.CategoryResponse;
import com.readora.catalog.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock private CatalogService catalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryController(catalogService)).build();
    }

    @Test
    void getTree_delegatesToService() throws Exception {
        when(catalogService.getCategoryTree()).thenReturn(List.of(new CategoryResponse(UUID.randomUUID(), "Fiction", "fiction", 1, List.of())));

        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
    }
}
