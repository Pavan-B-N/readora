package com.readora.catalog.controller;

import com.readora.catalog.dto.BookDetailResponse;
import com.readora.catalog.dto.PageResponse;
import com.readora.catalog.exception.BookNotFoundException;
import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.sharedcore.security.CurrentUserContext;
import com.readora.catalog.service.CatalogService;
import com.readora.catalog.service.VirtualContentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock private CatalogService catalogService;
    @Mock private VirtualContentService virtualContentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookController(catalogService, virtualContentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void search_delegatesToServiceWithParsedParams() throws Exception {
        when(catalogService.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/books").param("q", "spring"))
                .andExpect(status().isOk());
    }

    @Test
    void purchased_anonymousCaller_returnsEmptyWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/books/purchased"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void library_anonymousCaller_returnsEmptyWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/books/library"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void recommended_anonymousCaller_returnsEmptyWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/books/recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void purchased_authenticatedCaller_delegatesToService() throws Exception {
        UUID userId = UUID.randomUUID();
        CurrentUserContext.set(userId, List.of("CUSTOMER"));
        when(catalogService.getPurchasedBooks(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/purchased")).andExpect(status().isOk());
    }

    @Test
    void library_authenticatedCaller_delegatesToService() throws Exception {
        UUID userId = UUID.randomUUID();
        CurrentUserContext.set(userId, List.of("CUSTOMER"));
        when(catalogService.getLibrary(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/library")).andExpect(status().isOk());
    }

    @Test
    void recommended_authenticatedCaller_delegatesToService() throws Exception {
        UUID userId = UUID.randomUUID();
        CurrentUserContext.set(userId, List.of("CUSTOMER"));
        when(catalogService.getRecommendations(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/recommended")).andExpect(status().isOk());
    }

    @Test
    void getDetail_unexpectedException_mapsTo500() throws Exception {
        when(catalogService.getDetail(any(), any())).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    @Test
    void batch_delegatesToService() throws Exception {
        when(catalogService.getBooksByIds(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/batch").param("ids", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void suggest_delegatesToService() throws Exception {
        when(catalogService.suggest(eq("spr"), eq(8), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/suggest").param("q", "spr"))
                .andExpect(status().isOk());
    }

    @Test
    void getDetail_notFound_mapsTo404() throws Exception {
        when(catalogService.getDetail(any(), any())).thenThrow(new BookNotFoundException());

        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("BOOK_NOT_FOUND"));
    }

    @Test
    void getDetail_found_returns200() throws Exception {
        when(catalogService.getDetail(any(), any())).thenReturn(org.mockito.Mockito.mock(BookDetailResponse.class));

        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getRelated_delegatesToService() throws Exception {
        when(catalogService.getRelated(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID() + "/related"))
                .andExpect(status().isOk());
    }

    @Test
    void checkIsbn_delegatesToService() throws Exception {
        when(catalogService.existsByIsbn13("9780000000001")).thenReturn(true);

        mockMvc.perform(get("/api/v1/books/check-isbn").param("isbn", "9780000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void read_ownedBook_streamsContent() throws Exception {
        UUID userId = UUID.randomUUID();
        CurrentUserContext.set(userId, List.of("CUSTOMER"));
        when(virtualContentService.getContent(eq(userId), any())).thenReturn(new ByteArrayResource("data".getBytes()));

        mockMvc.perform(get("/api/v1/books/" + UUID.randomUUID() + "/read"))
                .andExpect(status().isOk());
    }
}
