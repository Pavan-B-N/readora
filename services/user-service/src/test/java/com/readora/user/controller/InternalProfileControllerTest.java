package com.readora.user.controller;

import com.readora.user.service.UserService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalProfileControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalProfileController(userService)).build();
    }

    @Test
    void getDisplayName_returnsNameFromService() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getDisplayName(userId)).thenReturn("A Reader");

        mockMvc.perform(get("/internal/profiles/" + userId + "/display-name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("A Reader"));
    }

    @Test
    void getDisplayName_noProfile_returnsNullBody() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getDisplayName(userId)).thenReturn(null);

        mockMvc.perform(get("/internal/profiles/" + userId + "/display-name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").doesNotExist());
    }

    @Test
    void getRecentBookViews_defaultsLimitTo20() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(userService.getRecentBookViewIds(userId, 20)).thenReturn(List.of(bookId));

        mockMvc.perform(get("/internal/profiles/" + userId + "/recent-book-views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(bookId.toString()));
    }

    @Test
    void getRecentSearches_respectsExplicitLimit() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getRecentSearchTerms(userId, 5)).thenReturn(List.of("spring boot"));

        mockMvc.perform(get("/internal/profiles/" + userId + "/recent-searches?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("spring boot"));
    }
}
