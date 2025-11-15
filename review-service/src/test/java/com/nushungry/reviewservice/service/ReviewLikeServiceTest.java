package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.document.ReviewLikeDocument;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.repository.ReviewLikeRepository;
import com.nushungry.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewLikeServiceTest {

    @Mock
    private ReviewLikeRepository reviewLikeRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewLikeService reviewLikeService;

    private ReviewDocument testReview;
    private ReviewLikeDocument testLike;

    @BeforeEach
    void setUp() {
        testReview = ReviewDocument.builder()
                .id("review1")
                .stallId(1L)
                .userId("user1")
                .likesCount(5)
                .build();

        testLike = new ReviewLikeDocument();
        testLike.setId("like1");
        testLike.setReviewId("review1");
        testLike.setUserId("user2");
        testLike.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testToggleLikeWhenNotLiked() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));
        when(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2")).thenReturn(false);
        when(reviewLikeRepository.save(any(ReviewLikeDocument.class))).thenReturn(testLike);
        when(reviewRepository.save(any(ReviewDocument.class))).thenReturn(testReview);

        boolean result = reviewLikeService.toggleLike("review1", "user2");

        assertThat(result).isTrue();
        verify(reviewLikeRepository).save(any(ReviewLikeDocument.class));
        verify(reviewRepository).save(any(ReviewDocument.class));
        assertThat(testReview.getLikesCount()).isEqualTo(6);
    }

    @Test
    void testToggleLikeWhenAlreadyLiked() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));
        when(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2")).thenReturn(true);
        when(reviewRepository.save(any(ReviewDocument.class))).thenReturn(testReview);

        boolean result = reviewLikeService.toggleLike("review1", "user2");

        assertThat(result).isFalse();
        verify(reviewLikeRepository).deleteByReviewIdAndUserId("review1", "user2");
        verify(reviewRepository).save(any(ReviewDocument.class));
        assertThat(testReview.getLikesCount()).isEqualTo(4);
    }

    @Test
    void testToggleLikeReviewNotFound() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewLikeService.toggleLike("review1", "user2"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");

        verify(reviewLikeRepository, never()).save(any());
        verify(reviewLikeRepository, never()).deleteByReviewIdAndUserId(anyString(), anyString());
    }

    @Test
    void testIsLikedByUserTrue() {
        when(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2")).thenReturn(true);

        boolean result = reviewLikeService.isLikedByUser("review1", "user2");

        assertThat(result).isTrue();
        verify(reviewLikeRepository).existsByReviewIdAndUserId("review1", "user2");
    }

    @Test
    void testIsLikedByUserFalse() {
        when(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2")).thenReturn(false);

        boolean result = reviewLikeService.isLikedByUser("review1", "user2");

        assertThat(result).isFalse();
        verify(reviewLikeRepository).existsByReviewIdAndUserId("review1", "user2");
    }

    @Test
    void testGetLikeCount() {
        when(reviewLikeRepository.countByReviewId("review1")).thenReturn(10L);

        long count = reviewLikeService.getLikeCount("review1");

        assertThat(count).isEqualTo(10L);
        verify(reviewLikeRepository).countByReviewId("review1");
    }

    @Test
    void testToggleLikeDecrementsCountToZero() {
        testReview.setLikesCount(1);
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));
        when(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2")).thenReturn(true);
        when(reviewRepository.save(any(ReviewDocument.class))).thenReturn(testReview);

        reviewLikeService.toggleLike("review1", "user2");

        assertThat(testReview.getLikesCount()).isEqualTo(0);
    }

    @Test
    void testToggleLikeIncrementsFromZero() {
        testReview.setLikesCount(0);
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));
        when(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2")).thenReturn(false);
        when(reviewLikeRepository.save(any(ReviewLikeDocument.class))).thenReturn(testLike);
        when(reviewRepository.save(any(ReviewDocument.class))).thenReturn(testReview);

        reviewLikeService.toggleLike("review1", "user2");

        assertThat(testReview.getLikesCount()).isEqualTo(1);
    }
}
