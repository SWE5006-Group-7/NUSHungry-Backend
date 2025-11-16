package com.nushungry.cafeteriaservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.service.CafeteriaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AdminCafeteriaController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.nushungry.cafeteriaservice.config.SecurityConfig.class,
                com.nushungry.cafeteriaservice.filter.JwtAuthenticationFilter.class
            }
        )
    }
)
class AdminCafeteriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CafeteriaService cafeteriaService;

    @Test
    void create_ShouldReturnCreatedCafeteria() throws Exception {
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("New Cafeteria");
        cafeteria.setLocation("Test Location");

        Cafeteria saved = new Cafeteria();
        saved.setId(1L);
        saved.setName("New Cafeteria");
        saved.setLocation("Test Location");

        when(cafeteriaService.save(any(Cafeteria.class))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/cafeterias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cafeteria)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("New Cafeteria"));

        verify(cafeteriaService, times(1)).save(any(Cafeteria.class));
    }

    @Test
    void update_WhenExists_ShouldReturnUpdatedCafeteria() throws Exception {
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Updated Cafeteria");
        cafeteria.setLocation("Updated Location");

        Cafeteria existing = new Cafeteria();
        existing.setId(1L);

        Cafeteria saved = new Cafeteria();
        saved.setId(1L);
        saved.setName("Updated Cafeteria");
        saved.setLocation("Updated Location");

        when(cafeteriaService.findById(1L)).thenReturn(existing);
        when(cafeteriaService.save(any(Cafeteria.class))).thenReturn(saved);

        mockMvc.perform(put("/api/admin/cafeterias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cafeteria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Updated Cafeteria"));

        verify(cafeteriaService, times(1)).findById(1L);
        verify(cafeteriaService, times(1)).save(any(Cafeteria.class));
    }

    @Test
    void update_WhenNotExists_ShouldReturn404() throws Exception {
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Updated Cafeteria");

        when(cafeteriaService.findById(999L)).thenReturn(null);

        mockMvc.perform(put("/api/admin/cafeterias/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cafeteria)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(cafeteriaService, times(1)).findById(999L);
        verify(cafeteriaService, never()).save(any(Cafeteria.class));
    }

    @Test
    void delete_ShouldReturnSuccess() throws Exception {
        Cafeteria existing = new Cafeteria();
        existing.setId(1L);

        when(cafeteriaService.findById(1L)).thenReturn(existing);
        doNothing().when(cafeteriaService).deleteById(1L);

        mockMvc.perform(delete("/api/admin/cafeterias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cafeteriaService, times(1)).findById(1L);
        verify(cafeteriaService, times(1)).deleteById(1L);
    }
}
