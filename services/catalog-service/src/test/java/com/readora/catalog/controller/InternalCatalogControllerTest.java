package com.readora.catalog.controller;

import com.readora.catalog.dto.BookAvailabilityRequest;
import com.readora.catalog.dto.BookAvailabilityResponse;
import com.readora.catalog.dto.BookCoverLookupRequest;
import com.readora.catalog.dto.BookCoverLookupResponse;
import com.readora.catalog.dto.BookExportPage;
import com.readora.catalog.dto.BookLookupRequest;
import com.readora.catalog.dto.BookLookupResponse;
import com.readora.catalog.dto.MarkEmbeddedRequest;
import com.readora.catalog.dto.StoreResponse;
import com.readora.catalog.dto.VirtualEditionLookupRequest;
import com.readora.catalog.dto.VirtualEditionLookupResponse;
import com.readora.sharedcore.exception.GlobalExceptionHandler;
import com.readora.catalog.exception.StoreNotFoundException;
import com.readora.catalog.service.InternalCatalogService;
import com.readora.catalog.service.VirtualContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalCatalogControllerTest {

    @Mock private InternalCatalogService internalCatalogService;
    @Mock private VirtualContentService virtualContentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalCatalogController(internalCatalogService, virtualContentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exportBooks_delegatesToService() throws Exception {
        when(internalCatalogService.exportBooks(any(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(new BookExportPage(List.of(), 0));

        mockMvc.perform(get("/internal/books/export")).andExpect(status().isOk());
    }

    @Test
    void markEmbedded_returns204() throws Exception {
        mockMvc.perform(post("/internal/books/embedded")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new MarkEmbeddedRequest(List.of(UUID.randomUUID())))))
                .andExpect(status().isNoContent());
    }

    @Test
    void lookupVirtualEditions_delegatesToService() throws Exception {
        when(internalCatalogService.lookupVirtualEditions(any())).thenReturn(new VirtualEditionLookupResponse(List.of()));

        mockMvc.perform(post("/internal/virtual-editions/lookup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new VirtualEditionLookupRequest(List.of()))))
                .andExpect(status().isOk());
    }

    @Test
    void lookupBooks_delegatesToService() throws Exception {
        when(internalCatalogService.lookupBooks(any())).thenReturn(new BookLookupResponse(List.of()));

        mockMvc.perform(post("/internal/books/lookup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BookLookupRequest(List.of()))))
                .andExpect(status().isOk());
    }

    @Test
    void checkAvailability_delegatesToService() throws Exception {
        when(internalCatalogService.checkAvailability(any(), any())).thenReturn(new BookAvailabilityResponse(List.of()));

        mockMvc.perform(post("/internal/books/availability")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BookAvailabilityRequest(List.of(), null))))
                .andExpect(status().isOk());
    }

    @Test
    void lookupCovers_delegatesToService() throws Exception {
        when(internalCatalogService.lookupCovers(any())).thenReturn(new BookCoverLookupResponse(List.of()));

        mockMvc.perform(post("/internal/books/covers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new BookCoverLookupRequest(List.of()))))
                .andExpect(status().isOk());
    }

    @Test
    void findStore_notFound_mapsTo404() throws Exception {
        when(internalCatalogService.findStore(any())).thenThrow(new StoreNotFoundException());

        mockMvc.perform(get("/internal/stores/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void isOwned_delegatesToVirtualContentService() throws Exception {
        UUID userId = UUID.randomUUID();
        when(virtualContentService.isOwned(org.mockito.ArgumentMatchers.eq(userId), any())).thenReturn(true);

        mockMvc.perform(get("/internal/books/" + UUID.randomUUID() + "/owned").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owned").value(true));
    }

    @Test
    void getContent_delegatesToVirtualContentService() throws Exception {
        when(virtualContentService.getContentForInternalUse(any())).thenReturn(new ByteArrayResource("x".getBytes()));

        mockMvc.perform(get("/internal/books/" + UUID.randomUUID() + "/content"))
                .andExpect(status().isOk());
    }
}
