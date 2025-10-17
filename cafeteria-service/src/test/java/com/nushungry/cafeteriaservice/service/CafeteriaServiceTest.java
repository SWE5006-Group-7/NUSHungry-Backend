package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.repository.CafeteriaRepository;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CafeteriaServiceTest {

    @Mock
    private CafeteriaRepository cafeteriaRepository;

    @Mock
    private StallRepository stallRepository;

    @InjectMocks
    private CafeteriaService cafeteriaService;

    @Test
    void save_null_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cafeteriaService.save(null));
        assertEquals("Cafeteria must not be null", ex.getMessage());
    }

    @Test
    void findAll_returnsList() {
        when(cafeteriaRepository.findAll()).thenReturn(List.of(new Cafeteria(), new Cafeteria()));
        var list = cafeteriaService.findAll();
        assertEquals(2, list.size());
        verify(cafeteriaRepository, times(1)).findAll();
    }

    @Test
    void findById_notFound() {
        when(cafeteriaRepository.findById(1L)).thenReturn(Optional.empty());
        Optional<Cafeteria> res = cafeteriaService.findById(1L);
        assertTrue(res.isEmpty());
    }
}
