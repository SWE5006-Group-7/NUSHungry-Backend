package com.nushungry.cafeteriaservice.listener;

import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewEventListenerTest {

    @Mock
    private StallRepository stallRepository;

    @InjectMocks
    private ReviewEventListener listener;

    @BeforeEach
    void setUp() {
    }

    @Test
    void handleReviewEvent_updatesExistingStall() {
        Stall s = new Stall();
        s.setId(42L);
        s.setAvgRating(3.0);
        s.setAvgPrice(5.0);

        when(stallRepository.findById(42L)).thenReturn(Optional.of(s));

        String msg = "{\"stallId\":42, \"avgRating\":4.5, \"avgPrice\":8.5}";
        listener.handleReviewEvent(msg);

        ArgumentCaptor<Stall> captor = ArgumentCaptor.forClass(Stall.class);
        verify(stallRepository, times(1)).save(captor.capture());
        Stall saved = captor.getValue();
        assertEquals(4.5, saved.getAvgRating());
        assertEquals(8.5, saved.getAvgPrice());
    }

    @Test
    void handleReviewEvent_nonExistingStall_noSave() {
        when(stallRepository.findById(100L)).thenReturn(Optional.empty());
        String msg = "{\"stallId\":100, \"avgRating\":4.0}";
        listener.handleReviewEvent(msg);
        verify(stallRepository, never()).save(any());
    }

    @Test
    void handleReviewEvent_malformed_doesNotThrow() {
        String bad = "not-a-json";
        listener.handleReviewEvent(bad);
        // no exception should propagate
    }
}
