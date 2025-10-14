package com.nushungry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.model.Stall;
import com.nushungry.service.StallService;
import com.nushungry.service.ImageService;
import com.nushungry.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StallController.class)
@AutoConfigureMockMvc(addFilters = false)
class StallControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StallService stallService;

    @MockBean
    private ImageService imageService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private com.nushungry.service.UserService userService;

    @MockBean
    private com.nushungry.service.SearchHistoryService searchHistoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenStalls_whenGetAll_thenReturnJsonArray() throws Exception {
        Stall stall1 = new Stall();
        stall1.setId(1L);
        stall1.setName("Stall 1");

        Stall stall2 = new Stall();
        stall2.setId(2L);
        stall2.setName("Stall 2");

        when(stallService.findAll()).thenReturn(Arrays.asList(stall1, stall2));

        mockMvc.perform(get("/api/stalls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Stall 1"))
                .andExpect(jsonPath("$[1].name").value("Stall 2"));

        verify(stallService).findAll();
    }

    @Test
    void givenStallId_whenGetById_thenReturnStall() throws Exception {
        Stall stall = new Stall();
        stall.setId(1L);
        stall.setName("Test Stall");

        when(stallService.findById(1L)).thenReturn(Optional.of(stall));

        mockMvc.perform(get("/api/stalls/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Stall"));

        verify(stallService).findById(1L);
    }

    @Test
    void givenInvalidId_whenGetById_thenReturn404() throws Exception {
        when(stallService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/stalls/99"))
                .andExpect(status().isNotFound());

        verify(stallService).findById(99L);
    }

    @Test
    void givenValidStall_whenCreate_thenReturnCreatedStall() throws Exception {
        Stall stall = new Stall();
        stall.setId(1L);
        stall.setName("New Stall");

        when(stallService.save(any(Stall.class))).thenReturn(stall);

        mockMvc.perform(post("/api/stalls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stall)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Stall"));

        verify(stallService).save(any(Stall.class));
    }
}
