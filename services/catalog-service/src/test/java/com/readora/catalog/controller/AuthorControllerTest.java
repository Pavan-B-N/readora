package com.readora.catalog.controller;

import com.readora.catalog.dto.AuthorResponse;
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
class AuthorControllerTest {

    @Mock private CatalogService catalogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthorController(catalogService)).build();
    }

    @Test
    void getAll_delegatesToService() throws Exception {
        when(catalogService.getAllAuthors()).thenReturn(List.of(new AuthorResponse(UUID.randomUUID(), "Name", "name", null, null)));

        mockMvc.perform(get("/api/v1/authors")).andExpect(status().isOk());
    }
}
