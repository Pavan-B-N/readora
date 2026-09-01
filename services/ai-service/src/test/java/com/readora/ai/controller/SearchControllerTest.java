package com.readora.ai.controller;

import com.readora.ai.dto.SearchResponse;
import com.readora.ai.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock private SearchService searchService;

    private SearchController controller;

    @BeforeEach
    void setUp() {
        controller = new SearchController(searchService);
    }

    @Test
    void search_delegatesToServiceWithTheGivenQueryAndLimit() {
        SearchResponse response = new SearchResponse("clean code", List.of());
        when(searchService.search("clean code", 10)).thenReturn(response);

        var result = controller.search("clean code", 10);

        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void search_clampsLimitAt50() {
        when(searchService.search("q", 50)).thenReturn(new SearchResponse("q", List.of()));

        controller.search("q", 500);

        verify(searchService).search("q", 50);
    }
}
