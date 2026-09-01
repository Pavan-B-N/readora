package com.readora.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.readora.catalog.dto.CreateBookRequest;
import com.readora.catalog.dto.IdResponse;
import com.readora.catalog.dto.AdminBookDetailResponse;
import com.readora.catalog.dto.UpdateBookRequest;
import com.readora.catalog.dto.UpdateInventoryRequest;
import com.readora.catalog.dto.UpsertVirtualEditionRequest;
import com.readora.catalog.entity.VirtualFileFormat;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.catalog.exception.GlobalExceptionHandler;
import com.readora.catalog.service.AdminBookService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBookControllerTest {

    @Mock private AdminBookService adminBookService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBookController(adminBookService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getBookForEdit_notFound_mapsTo404() throws Exception {
        when(adminBookService.getBookForEdit(any())).thenThrow(new BookNotFoundException());

        mockMvc.perform(get("/api/v1/admin/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBook_valid_returns201() throws Exception {
        when(adminBookService.createBook(any())).thenReturn(new IdResponse(UUID.randomUUID()));

        CreateBookRequest request = new CreateBookRequest(
                "9780000000001", "T", "D", null, null, null, null, List.of(UUID.randomUUID()),
                "en", 1, null, BigDecimal.TEN, "INR", null);

        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createBook_noAuthors_returns400() throws Exception {
        CreateBookRequest request = new CreateBookRequest(
                "9780000000001", "T", "D", null, null, null, null, List.of(),
                "en", 1, null, BigDecimal.TEN, "INR", null);

        mockMvc.perform(post("/api/v1/admin/books")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBookForEdit_found_returns200() throws Exception {
        when(adminBookService.getBookForEdit(any())).thenReturn(org.mockito.Mockito.mock(AdminBookDetailResponse.class));

        mockMvc.perform(get("/api/v1/admin/books/" + UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void updateBook_valid_returns204() throws Exception {
        UpdateBookRequest request = new UpdateBookRequest(
                "T", "D", null, null, null, null, "en", 1, null, BigDecimal.TEN, "INR", null, true);

        mockMvc.perform(put("/api/v1/admin/books/" + UUID.randomUUID())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void upsertVirtualEdition_valid_returns204() throws Exception {
        UpsertVirtualEditionRequest request = new UpsertVirtualEditionRequest(
                "s3://bucket/key.epub", VirtualFileFormat.EPUB, null, new BigDecimal("199.00"), "INR");

        mockMvc.perform(put("/api/v1/admin/books/" + UUID.randomUUID() + "/virtual-edition")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateInventory_valid_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/admin/books/" + UUID.randomUUID() + "/inventory")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateInventoryRequest(10, 2))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateVirtualEdition_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/books/" + UUID.randomUUID() + "/virtual-edition"))
                .andExpect(status().isNoContent());
    }
}
