package com.nushungry.cafeteriaservice.service;

import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.junit.jupiter.api.BeforeEach;
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
public class StallServiceTest {

    @Mock
    private StallRepository stallRepository;

    @InjectMocks
    private StallService stallService;

    @BeforeEach
    void setUp() {
        // MockitoExtension handles initialization
    }

    @Test
    void save_null_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> stallService.save(null));
        assertEquals("Stall must not be null", ex.getMessage());
    }

    @Test
    void findById_returnsOptional() {
        Stall s = new Stall();
        s.setId(1L);
        when(stallRepository.findById(1L)).thenReturn(Optional.of(s));

        Optional<Stall> res = stallService.findById(1L);
        assertTrue(res.isPresent());
        assertEquals(1L, res.get().getId());
        verify(stallRepository, times(1)).findById(1L);
    }

    @Test
    void findAll_returnsList() {
        when(stallRepository.findAll()).thenReturn(List.of(new Stall(), new Stall()));
        var list = stallService.findAll();
        assertNotNull(list);
        assertEquals(2, list.size());
        verify(stallRepository, times(1)).findAll();
    }
}
