package com.readora.user.controller;

import com.readora.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalAdminControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalAdminController(userService)).build();
    }

    @Test
    void getAdminStore_returnsStoreId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        when(userService.getAdminStoreId(userId)).thenReturn(storeId);

        mockMvc.perform(get("/internal/admin-users/" + userId + "/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId.toString()));
    }

    @Test
    void getAdminStore_unassigned_returnsNullBody() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getAdminStoreId(userId)).thenReturn(null);

        mockMvc.perform(get("/internal/admin-users/" + userId + "/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").doesNotExist());
    }

    @Test
    void getStoreAdmin_returnsAssignedAdminUserId() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        when(userService.getAdminUserIdForStore(storeId)).thenReturn(adminUserId);

        mockMvc.perform(get("/internal/admin-users/by-store/" + storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(adminUserId.toString()));
    }
}
