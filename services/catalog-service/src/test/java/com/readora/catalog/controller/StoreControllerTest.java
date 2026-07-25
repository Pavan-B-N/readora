package com.readora.catalog.controller;

import com.readora.catalog.entity.Store;
import com.readora.catalog.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock private StoreRepository storeRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StoreController(storeRepository)).build();
    }

    @Test
    void listStores_mapsActiveStores() throws Exception {
        Store store = new Store("Name", "City", "L1", null, "State", "000000", "IN");
        Field idField = Store.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(store, java.util.UUID.randomUUID());
        when(storeRepository.findAllByIsActiveTrueOrderByName()).thenReturn(List.of(store));

        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Name"));
    }
}
