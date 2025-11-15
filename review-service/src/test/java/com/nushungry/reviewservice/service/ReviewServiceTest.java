package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.CreateReviewRequest;
import com.nushungry.reviewservice.dto.ReviewResponse;
import com.nushungry.reviewservice.dto.UpdateReviewRequest;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.UnauthorizedException;
import com.nushungry.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewLikeService reviewLikeService;

    @Mock
    private RatingCalculationService ratingCalculationService;

    @Mock
    private PriceCalculationService priceCalculationService;

    @InjectMocks
    private ReviewService reviewService;

    private ReviewDocument testReview;
    private CreateReviewRequest createRequest;
    private UpdateReviewRequest updateRequest;

    @BeforeEach
    void setUp() {
        testReview = ReviewDocument.builder()
                .id("review1")
                .stallId(1L)
                .stallName("Test Stall")
                .userId("user1")
                .username("Test User")
                .userAvatarUrl("avatar.jpg")
                .rating(5)
                .comment("Great food!")
                .imageUrls(Arrays.asList("image1.jpg", "image2.jpg"))
                .totalCost(20.0)
                .numberOfPeople(2)
                .likesCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .stallName("Test Stall")
                .rating(5)
                .comment("Great food!")
                .imageUrls(Arrays.asList("image1.jpg", "image2.jpg"))
                .totalCost(20.0)
                .numberOfPeople(2)
                .build();

        updateRequest = UpdateReviewRequest.builder()
                .rating(4)
                .comment("Updated comment")
                .build();
    }

    @Test
    void testCreateReview() {
        when(reviewRepository.save(any(ReviewDocument.class))).thenReturn(testReview);
        when(reviewLikeService.isLikedByUser(anyString(), anyString())).thenReturn(false);

        ReviewResponse response = reviewService.createReview(
                createRequest, "user1", "Test User", "avatar.jpg");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("review1");
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Great food!");

        verify(reviewRepository).save(any(ReviewDocument.class));
        verify(ratingCalculationService).calculateAndPublishRating(1L);
        verify(priceCalculationService).calculateAndPublishPrice(1L);
    }

    @Test
    void testUpdateReview() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(ReviewDocument.class))).thenReturn(testReview);
        when(reviewLikeService.isLikedByUser(anyString(), anyString())).thenReturn(false);

        ReviewResponse response = reviewService.updateReview("review1", updateRequest, "user1");

        assertThat(response).isNotNull();
        verify(reviewRepository).save(any(ReviewDocument.class));
        verify(ratingCalculationService).calculateAndPublishRating(1L);
        verify(priceCalculationService).calculateAndPublishPrice(1L);
    }

    @Test
    void testUpdateReviewNotFound() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview("review1", updateRequest, "user1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");

        verify(reviewRepository, never()).save(any(ReviewDocument.class));
    }

    @Test
    void testUpdateReviewUnauthorized() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));

        assertThatThrownBy(() -> reviewService.updateReview("review1", updateRequest, "user2"))
                .isInstanceOf(UnauthorizedException.class);

        verify(reviewRepository, never()).save(any(ReviewDocument.class));
    }

    @Test
    void testDeleteReview() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).delete(any(ReviewDocument.class));

        reviewService.deleteReview("review1", "user1");

        verify(reviewRepository).delete(testReview);
        verify(ratingCalculationService).calculateAndPublishRating(1L);
        verify(priceCalculationService).calculateAndPublishPrice(1L);
    }

    @Test
    void testDeleteReviewNotFound() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview("review1", "user1"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void testDeleteReviewUnauthorized() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));

        assertThatThrownBy(() -> reviewService.deleteReview("review1", "user2"))
                .isInstanceOf(UnauthorizedException.class);

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void testGetReviewById() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.of(testReview));
        when(reviewLikeService.isLikedByUser("review1", "user1")).thenReturn(true);

        ReviewResponse response = reviewService.getReviewById("review1", "user1");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("review1");
        assertThat(response.getIsLikedByCurrentUser()).isTrue();

        verify(reviewRepository).findById("review1");
    }

    @Test
    void testGetReviewByIdNotFound() {
        when(reviewRepository.findById("review1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getReviewById("review1", "user1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void testGetReviewsByStallIdWithDefaultSort() {
        List<ReviewDocument> reviews = Arrays.asList(testReview);
        Page<ReviewDocument> page = new PageImpl<>(reviews);
        Pageable pageable = PageRequest.of(0, 10);

        when(reviewRepository.findByStallIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);
        when(reviewLikeService.isLikedByUser(anyString(), anyString())).thenReturn(false);

        Page<ReviewResponse> result = reviewService.getReviewsByStallId(1L, "createdAt", "user1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("review1");

        verify(reviewRepository).findByStallIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    void testGetReviewsByStallIdSortByLikes() {
        List<ReviewDocument> reviews = Arrays.asList(testReview);
        Page<ReviewDocument> page = new PageImpl<>(reviews);
        Pageable pageable = PageRequest.of(0, 10);

        when(reviewRepository.findByStallIdOrderByLikesCountDesc(1L, pageable)).thenReturn(page);
        when(reviewLikeService.isLikedByUser(anyString(), anyString())).thenReturn(false);

        Page<ReviewResponse> result = reviewService.getReviewsByStallId(1L, "likes", "user1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("review1");

        verify(reviewRepository).findByStallIdOrderByLikesCountDesc(1L, pageable);
    }

    @Test
    void testGetReviewsByUserId() {
        List<ReviewDocument> reviews = Arrays.asList(testReview);
        Page<ReviewDocument> page = new PageImpl<>(reviews);
        Pageable pageable = PageRequest.of(0, 10);

        when(reviewRepository.findByUserIdOrderByCreatedAtDesc("user1", pageable)).thenReturn(page);
        when(reviewLikeService.isLikedByUser(anyString(), anyString())).thenReturn(false);

        Page<ReviewResponse> result = reviewService.getReviewsByUserId("user1", "user1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo("user1");

        verify(reviewRepository).findByUserIdOrderByCreatedAtDesc("user1", pageable);
    }
}
