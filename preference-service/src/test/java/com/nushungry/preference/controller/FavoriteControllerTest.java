package com.nushungry.preference.controller;

import com.nushungry.preference.service.FavoriteService;
import com.nushungry.preference.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FavoriteController 测试
 *
 * 使用 @WebMvcTest 进行轻量级 Controller 测试
 * 排除 Security 配置以简化测试
 */
@WebMvcTest(
    controllers = FavoriteController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.nushungry.preference.filter.JwtAuthenticationFilter.class
            }
        )
    }
)
class FavoriteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testAddFavorite() throws Exception {
        String requestBody = "{\"userId\": 1, \"stallId\": 2}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(favoriteService, times(1)).addFavorite(1L, 2L);
    }

    @Test
    void testRemoveFavorite() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/favorites")
                .param("userId", "1")
                .param("stallId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
        verify(favoriteService, times(1)).removeFavorite(1L, 2L);
    }

    @Test
    void testListFavorites() throws Exception {
        when(favoriteService.listFavorites(1L)).thenReturn(List.of(2L, 3L));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/favorites/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(2L))
                .andExpect(jsonPath("$[1]").value(3L));
    }

    @Test
    void testBatchRemove() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/favorites/batchRemove")
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
        mockMvc.perform(MockMvcRequestBuilders.get("/api/favorites/sorted")
                .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(3L))
                .andExpect(jsonPath("$[1]").value(2L));
    }
}

