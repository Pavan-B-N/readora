package com.readora.notification.controller;

import com.readora.notification.dto.NotificationResponse;
import com.readora.notification.exception.GlobalExceptionHandler;
import com.readora.notification.exception.NotificationNotFoundException;
import com.readora.notification.security.CurrentUserContext;
import com.readora.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        CurrentUserContext.set(UUID.randomUUID());
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void list_delegatesToService() throws Exception {
        when(notificationService.list(any(), any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isOk());
    }

    @Test
    void unreadCount_delegatesToService() throws Exception {
        when(notificationService.unreadCount(any())).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markRead_notFound_mapsTo404() throws Exception {
        org.mockito.Mockito.doThrow(new NotificationNotFoundException()).when(notificationService).markRead(any(), any());

        mockMvc.perform(put("/api/v1/notifications/" + UUID.randomUUID() + "/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markRead_found_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/" + UUID.randomUUID() + "/read"))
                .andExpect(status().isNoContent());
    }

    @Test
    void markAllRead_returns204() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all")).andExpect(status().isNoContent());
    }
}
