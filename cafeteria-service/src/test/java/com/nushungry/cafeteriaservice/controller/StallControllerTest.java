package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.service.StallService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = StallController.class,
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
class StallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StallService stallService;

    @Test
    void getAllStalls_ShouldReturnStallList() throws Exception {
        Stall stall1 = new Stall();
        stall1.setId(1L);
        stall1.setName("Stall 1");

        Stall stall2 = new Stall();
        stall2.setId(2L);
        stall2.setName("Stall 2");

        when(stallService.findAll()).thenReturn(Arrays.asList(stall1, stall2));

        mockMvc.perform(get("/api/stalls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Stall 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Stall 2"));
    }

    @Test
    void getStall_WhenExists_ShouldReturnStall() throws Exception {
        Stall stall = new Stall();
        stall.setId(1L);
        stall.setName("Test Stall");

        when(stallService.findById(1L)).thenReturn(Optional.of(stall));

        mockMvc.perform(get("/api/stalls/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Stall"));
    }

    @Test
    void getStall_WhenNotExists_ShouldReturn404() throws Exception {
        when(stallService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/stalls/999"))
                .andExpect(status().isNotFound());
    }
}
