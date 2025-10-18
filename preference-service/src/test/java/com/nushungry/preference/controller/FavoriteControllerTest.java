package com.nushungry.preference.controller;

import com.nushungry.preference.service.FavoriteService;
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

@WebMvcTest(FavoriteController.class)
class FavoriteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteService favoriteService;

    @Test
    void testAddFavorite() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/preference/favorite/add")
                .param("userId", "1")
                .param("stallId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(favoriteService, times(1)).addFavorite(1L, 2L);
    }

    @Test
    void testRemoveFavorite() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/preference/favorite/remove")
                .param("userId", "1")
                .param("stallId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(favoriteService, times(1)).removeFavorite(1L, 2L);
    }

    @Test
    void testListFavorites() throws Exception {
        when(favoriteService.listFavorites(1L)).thenReturn(List.of(2L, 3L));
        mockMvc.perform(MockMvcRequestBuilders.get("/preference/favorite/list")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(2L))
                .andExpect(jsonPath("$[1]").value(3L));
    }

    @Test
    void testBatchRemove() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/preference/favorite/batchRemove")
                .param("userId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[2,3]"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(favoriteService, times(1)).batchRemove(eq(1L), anyList());
    }

    @Test
    void testSortedFavorites() throws Exception {
        when(favoriteService.sortedFavorites(1L)).thenReturn(List.of(3L, 2L));
        mockMvc.perform(MockMvcRequestBuilders.get("/preference/favorite/sorted")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(3L))
                .andExpect(jsonPath("$[1]").value(2L));
    }
}

