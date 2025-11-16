package com.nushungry.service;

import com.nushungry.model.Cafeteria;
import com.nushungry.repository.CafeteriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CafeteriaServiceUnitTest {
    @Mock
    private CafeteriaRepository cafeteriaRepository;

    @InjectMocks
    private CafeteriaService cafeteriaService;

    @Test
    void shouldCreateCafeteria() {
        // Given
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.setName("Cafeteria");
        cafeteria.setDescription("Description");
        cafeteria.setLongitude(90);
        cafeteria.setLatitude(90);
        cafeteria.setLocation("Building 9201921");
        cafeteria.setNearestBusStop("Biz 2");
        when(cafeteriaRepository.save(any(Cafeteria.class))).thenReturn(cafeteria);

        // When
        Cafeteria result = cafeteriaService.save(cafeteria);

        // Then
        assert(result.getName()).equals("Cafeteria");
        assert(result.getDescription()).equals("Description");
        assertThat(result.getLongitude()).isEqualTo(90);
        assertThat(result.getLatitude()).isEqualTo(90);
        assertThat(result.getLocation()).isEqualTo("Building 9201921");
        assertThat(result.getNearestBusStop()).isEqualTo("Biz 2");
        verify(cafeteriaRepository).save(any(Cafeteria.class));
    }
}
