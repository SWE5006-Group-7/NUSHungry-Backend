package com.nushungry.controller;

import com.nushungry.model.Cafeteria;
import com.nushungry.service.CafeteriaService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CafeteriaController.class, excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class})
// Exclude security auto-configuration for testing purposes
class CafeteriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CafeteriaService cafeteriaService;

    @Test
    void testGetAllCafeterias_single() throws Exception {
        Cafeteria c1 = new Cafeteria();
        c1.setId(1L);
        c1.setName("ACanteen");
        c1.setLocation("UTown");
        c1.setLatitude(1.300);
        c1.setLongitude(103.770);
        Mockito.when(cafeteriaService.findAll()).thenReturn(Arrays.asList(c1));

        mockMvc.perform(get("/api/cafeterias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("ACanteen"))
                .andExpect(jsonPath("$[0].location").value("UTown"))
                .andExpect(jsonPath("$[0].latitude").value(1.300))
                .andExpect(jsonPath("$[0].longitude").value(103.770));
    }

    @Test
    void testGetAllCafeterias_multiple() throws Exception {
        Cafeteria c1 = new Cafeteria();
        c1.setId(1L);
        c1.setName("ACanteen");
        Cafeteria c2 = new Cafeteria();
        c2.setId(2L);
        c2.setName("BCanteen");

        Mockito.when(cafeteriaService.findAll()).thenReturn(Arrays.asList(c1, c2));

        mockMvc.perform(get("/api/cafeterias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("ACanteen"))
                .andExpect(jsonPath("$[1].name").value("BCanteen"));
    }

    @Test
    void testGetAllCafeterias_empty() throws Exception {
        Mockito.when(cafeteriaService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/cafeterias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetAllCafeterias_exception() throws Exception {
        Mockito.when(cafeteriaService.findAll()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/cafeterias"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void testGetCafeteriaById_Found() throws Exception {
        Cafeteria c = new Cafeteria();
        c.setId(1L);
        c.setName("ACanteen");
        Mockito.when(cafeteriaService.findById(1L)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/api/cafeterias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ACanteen"));
    }

    @Test
    void testGetCafeteriaById_NotFound() throws Exception {
        Mockito.when(cafeteriaService.findById(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cafeterias/2"))
                .andExpect(status().isNotFound());
    }
}