package com.nushungry.service;

import com.nushungry.model.Stall;
import com.nushungry.repository.StallRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StallServiceUnitTest {

    @Mock
    private StallRepository stallRepository;

    @InjectMocks
    private StallService stallService;

    private final static com.nushungry.model.Cafeteria validCafeteria = new com.nushungry.model.Cafeteria() {{
        setId(1L);
        setName("Valid Cafeteria");
        setDescription("A valid cafeteria for testing");
        setLongitude(103.7744);
        setLatitude(1.2966);
        setLocation("Test Location");
        setNearestBusStop("Test Bus Stop");
    }};

    @Test
    void givenStalls_whenFindAll_thenReturnStalls() {
        // Given
        Stall stall1 = new Stall();
        stall1.setId(1L);
        stall1.setName("Stall 1");

        Stall stall2 = new Stall();
        stall2.setId(2L);
        stall2.setName("Stall 2");

        when(stallRepository.findAll()).thenReturn(Arrays.asList(stall1, stall2));

        // When
        List<Stall> result = stallService.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Stall::getName).containsExactly("Stall 1", "Stall 2");
        verify(stallRepository).findAll();
    }

    @Test
    void givenStallId_whenFindById_thenReturnStall() {
        // Given
        Stall stall = new Stall();
        stall.setId(1L);
        stall.setName("Test Stall");

        when(stallRepository.findById(1L)).thenReturn(Optional.of(stall));

        // When
        Optional<Stall> result = stallService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Stall");
        verify(stallRepository).findById(1L);
    }

    @Test
    void givenInvalidId_whenFindById_thenReturnEmptyOptional() {
        // Given
        when(stallRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        Optional<Stall> result = stallService.findById(99L);

        // Then
        assertThat(result).isEmpty();
        verify(stallRepository).findById(99L);
    }

    @Test
    void givenValidStall_whenSave_thenReturnSavedStall() {
        // Given
        Stall stall = new Stall();
        stall.setId(1L);
        stall.setName("Test Stall");
        stall.setCafeteria(validCafeteria);

        when(stallRepository.save(any(Stall.class))).thenReturn(stall);

        // When
        Stall result = stallService.save(stall);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Stall");
        verify(stallRepository).save(any(Stall.class));
    }

    @Test
    void givenNullStall_whenSave_thenThrowException() {
        assertThatThrownBy(() -> stallService.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Stall must not be null");
    }
}
