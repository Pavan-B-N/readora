package com.readora.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.dto.CreateAuthorRequest;
import com.readora.catalog.dto.CreateCategoryRequest;
import com.readora.catalog.dto.CreatePublisherRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.UpdateAuthorRequest;
import com.readora.catalog.dto.UpdateCategoryRequest;
import com.readora.catalog.exception.AuthorInUseException;
import com.readora.catalog.exception.CategoryNotFoundException;
import com.readora.catalog.exception.GlobalExceptionHandler;
import com.readora.catalog.service.AdminCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCatalogControllerTest {

    @Mock private AdminCatalogService adminCatalogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCatalogController(adminCatalogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCategory_valid_returns201() throws Exception {
        when(adminCatalogService.createCategory(any())).thenReturn(new IdResponse(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Fiction", "fiction", 1))))
                .andExpect(status().isCreated());
    }

    @Test
    void createCategory_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("", "fiction", 1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_notFound_mapsTo404() throws Exception {
        org.mockito.Mockito.doThrow(new CategoryNotFoundException()).when(adminCatalogService).deleteCategory(any());

        mockMvc.perform(delete("/api/v1/admin/categories/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_found_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/categories/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateCategory_valid_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/admin/categories/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest("Fiction", "fiction", 1))))
                .andExpect(status().isNoContent());
    }

    @Test
    void createPublisher_valid_returns201() throws Exception {
        when(adminCatalogService.createPublisher(any())).thenReturn(new IdResponse(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/admin/publishers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreatePublisherRequest("Penguin", "penguin"))))
                .andExpect(status().isCreated());
    }

    @Test
    void createAuthor_valid_returns201() throws Exception {
        when(adminCatalogService.createAuthor(any())).thenReturn(new IdResponse(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/admin/authors")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateAuthorRequest("Name", "name", null, null))))
                .andExpect(status().isCreated());
    }

    @Test
    void updateAuthor_valid_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/admin/authors/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateAuthorRequest("Name", "name", null, null))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAuthor_found_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/authors/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAuthor_stillCredited_mapsTo409() throws Exception {
        org.mockito.Mockito.doThrow(new AuthorInUseException()).when(adminCatalogService).deleteAuthor(any());

        mockMvc.perform(delete("/api/v1/admin/authors/" + UUID.randomUUID()))
                .andExpect(status().isConflict());
    }
}
