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
        s.setReviewCount(5);

        when(stallRepository.findById(42L)).thenReturn(Optional.of(s));

        String msg = "{\"stallId\":42, \"newAverageRating\":4.5, \"reviewCount\":10}";
        listener.handleReviewEvent(msg);

        ArgumentCaptor<Stall> captor = ArgumentCaptor.forClass(Stall.class);
        verify(stallRepository, times(1)).save(captor.capture());
        Stall saved = captor.getValue();
        assertEquals(4.5, saved.getAvgRating());
        assertEquals(10, saved.getReviewCount());
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

    @Test
    void handleReviewEvent_zeroRating_shouldUpdate() {
        // 测试评分为0的边界情况
        Stall s = new Stall();
        s.setId(1L);
        s.setAvgRating(3.5);
        s.setReviewCount(10);

        when(stallRepository.findById(1L)).thenReturn(Optional.of(s));

        String msg = "{\"stallId\":1, \"newAverageRating\":0.0, \"reviewCount\":0}";
        listener.handleReviewEvent(msg);

        ArgumentCaptor<Stall> captor = ArgumentCaptor.forClass(Stall.class);
        verify(stallRepository, times(1)).save(captor.capture());
        Stall saved = captor.getValue();
        assertEquals(0.0, saved.getAvgRating());
        assertEquals(0, saved.getReviewCount());
    }

    @Test
    void handleReviewEvent_nullRating_shouldNotUpdate() {
        // 测试评分为null时不更新的情况
        Stall s = new Stall();
        s.setId(2L);
        s.setAvgRating(4.0);
        s.setReviewCount(5);

        when(stallRepository.findById(2L)).thenReturn(Optional.of(s));

        String msg = "{\"stallId\":2, \"reviewCount\":5}"; // 没有newAverageRating字段
        listener.handleReviewEvent(msg);

        ArgumentCaptor<Stall> captor = ArgumentCaptor.forClass(Stall.class);
        verify(stallRepository, times(1)).save(captor.capture());
        Stall saved = captor.getValue();
        // 评分应该保持原值
        assertEquals(4.0, saved.getAvgRating());
        assertEquals(5, saved.getReviewCount());
    }

    @Test
    void handleReviewEvent_missingStallId_shouldNotUpdate() {
        // 测试缺少stallId的情况
        String msg = "{\"newAverageRating\":4.5, \"reviewCount\":10}"; // 缺少stallId
        listener.handleReviewEvent(msg);

        // 不应该调用repository的save方法
        verify(stallRepository, never()).save(any());
    }
}
