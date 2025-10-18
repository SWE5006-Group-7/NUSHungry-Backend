package com.nushungry.preference.controller;

import com.nushungry.preference.service.SearchHistoryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(SearchHistoryController.class)
class SearchHistoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchHistoryService searchHistoryService;

    @Test
    void testAddHistory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/preference/search-history/add")
                .param("userId", "1")
                .param("keyword", "noodle"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(searchHistoryService, times(1)).addHistory(1L, "noodle");
    }

    @Test
    void testListHistory() throws Exception {
        when(searchHistoryService.listHistory(1L)).thenReturn(List.of("noodle", "rice"));
        mockMvc.perform(MockMvcRequestBuilders.get("/preference/search-history/list")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("noodle"))
                .andExpect(jsonPath("$[1]").value("rice"));
    }

    @Test
    void testBatchRemove() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/preference/search-history/batchRemove")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"noodle\",\"rice\"]"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(searchHistoryService, times(1)).batchRemove(eq(1L), anyList());
    }

    @Test
    void testClearHistory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/preference/search-history/clear")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(searchHistoryService, times(1)).clearHistory(1L);
    }
}

