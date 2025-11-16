package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.service.CafeteriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CafeteriaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CafeteriaService cafeteriaService;

    @InjectMocks
    private CafeteriaController cafeteriaController;

    @BeforeEach
    void setUp() {
        // we'll initialize stubbed controller per-test where needed
    }

    @Test
    void getAllCafeterias_returnsOk() throws Exception {
        CafeteriaService stubService = new CafeteriaService(null, null) {
            @Override
            public List<Cafeteria> findAll() {
                return List.of(new Cafeteria());
            }
        };
        CafeteriaController controller = new CafeteriaController(stubService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/cafeterias").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void getCafeteriaById_notFound() throws Exception {
        CafeteriaService stubService = new CafeteriaService(null, null) {
            @Override
            public Cafeteria findById(Long id) {
                return null;
            }
        };
        CafeteriaController controller = new CafeteriaController(stubService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/cafeterias/1").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
}
