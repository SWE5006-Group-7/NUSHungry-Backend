package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.RatingDistributionResponse;
import com.nushungry.reviewservice.event.RatingChangedEvent;
import com.nushungry.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingCalculationServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private RatingCalculationService ratingCalculationService;

    private List<ReviewDocument> testReviews;

    @BeforeEach
    void setUp() {
        testReviews = Arrays.asList(
                createReview(5),
                createReview(4),
                createReview(5),
                createReview(3),
                createReview(4)
        );
    }

    @Test
    void testCalculateAndPublishRatingWithReviews() {
        when(reviewRepository.findByStallId(1L)).thenReturn(testReviews);

        ratingCalculationService.calculateAndPublishRating(1L);

        ArgumentCaptor<RatingChangedEvent> eventCaptor = ArgumentCaptor.forClass(RatingChangedEvent.class);
        verify(eventPublisherService).publishRatingChanged(eventCaptor.capture());

        RatingChangedEvent event = eventCaptor.getValue();
        assertThat(event.getStallId()).isEqualTo(1L);
        assertThat(event.getNewAverageRating()).isEqualTo(4.2);
        assertThat(event.getReviewCount()).isEqualTo(5L);
    }

    @Test
    void testCalculateAndPublishRatingWithNoReviews() {
        when(reviewRepository.findByStallId(1L)).thenReturn(Collections.emptyList());

        ratingCalculationService.calculateAndPublishRating(1L);

        ArgumentCaptor<RatingChangedEvent> eventCaptor = ArgumentCaptor.forClass(RatingChangedEvent.class);
        verify(eventPublisherService).publishRatingChanged(eventCaptor.capture());

        RatingChangedEvent event = eventCaptor.getValue();
        assertThat(event.getStallId()).isEqualTo(1L);
        assertThat(event.getNewAverageRating()).isEqualTo(0.0);
        assertThat(event.getReviewCount()).isEqualTo(0L);
    }

    @Test
    void testCalculateRatingWithAllFiveStars() {
        List<ReviewDocument> allFiveStars = Arrays.asList(
                createReview(5),
                createReview(5),
                createReview(5)
        );
        when(reviewRepository.findByStallId(1L)).thenReturn(allFiveStars);

        ratingCalculationService.calculateAndPublishRating(1L);

        ArgumentCaptor<RatingChangedEvent> eventCaptor = ArgumentCaptor.forClass(RatingChangedEvent.class);
        verify(eventPublisherService).publishRatingChanged(eventCaptor.capture());

        RatingChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAverageRating()).isEqualTo(5.0);
    }

    @Test
    void testCalculateRatingWithAllOneStars() {
        List<ReviewDocument> allOneStars = Arrays.asList(
                createReview(1),
                createReview(1),
                createReview(1)
        );
        when(reviewRepository.findByStallId(1L)).thenReturn(allOneStars);

        ratingCalculationService.calculateAndPublishRating(1L);

        ArgumentCaptor<RatingChangedEvent> eventCaptor = ArgumentCaptor.forClass(RatingChangedEvent.class);
        verify(eventPublisherService).publishRatingChanged(eventCaptor.capture());

        RatingChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAverageRating()).isEqualTo(1.0);
    }

    @Test
    void testGetRatingDistribution() {
        when(reviewRepository.findByStallId(1L)).thenReturn(testReviews);

        RatingDistributionResponse response = ratingCalculationService.getRatingDistribution(1L);

        assertThat(response.getStallId()).isEqualTo(1L);
        assertThat(response.getAverageRating()).isEqualTo(4.2);
        assertThat(response.getTotalReviews()).isEqualTo(5L);

        Map<Integer, Long> distribution = response.getDistribution();
        assertThat(distribution.get(5)).isEqualTo(2L);
        assertThat(distribution.get(4)).isEqualTo(2L);
        assertThat(distribution.get(3)).isEqualTo(1L);
        assertThat(distribution.get(2)).isEqualTo(0L);
        assertThat(distribution.get(1)).isEqualTo(0L);
    }

    @Test
    void testGetRatingDistributionWithNoReviews() {
        when(reviewRepository.findByStallId(1L)).thenReturn(Collections.emptyList());

        RatingDistributionResponse response = ratingCalculationService.getRatingDistribution(1L);

        assertThat(response.getStallId()).isEqualTo(1L);
        assertThat(response.getAverageRating()).isEqualTo(0.0);
        assertThat(response.getTotalReviews()).isEqualTo(0L);

        Map<Integer, Long> distribution = response.getDistribution();
        assertThat(distribution.get(1)).isEqualTo(0L);
        assertThat(distribution.get(2)).isEqualTo(0L);
        assertThat(distribution.get(3)).isEqualTo(0L);
        assertThat(distribution.get(4)).isEqualTo(0L);
        assertThat(distribution.get(5)).isEqualTo(0L);
    }

    @Test
    void testGetRatingDistributionWithMixedRatings() {
        List<ReviewDocument> mixedReviews = Arrays.asList(
                createReview(1),
                createReview(2),
                createReview(3),
                createReview(4),
                createReview(5)
        );
        when(reviewRepository.findByStallId(1L)).thenReturn(mixedReviews);

        RatingDistributionResponse response = ratingCalculationService.getRatingDistribution(1L);

        Map<Integer, Long> distribution = response.getDistribution();
        assertThat(distribution.get(1)).isEqualTo(1L);
        assertThat(distribution.get(2)).isEqualTo(1L);
        assertThat(distribution.get(3)).isEqualTo(1L);
        assertThat(distribution.get(4)).isEqualTo(1L);
        assertThat(distribution.get(5)).isEqualTo(1L);
        assertThat(response.getAverageRating()).isEqualTo(3.0);
    }

    @Test
    void testCalculateRatingWithSingleReview() {
        List<ReviewDocument> singleReview = Collections.singletonList(createReview(4));
        when(reviewRepository.findByStallId(1L)).thenReturn(singleReview);

        ratingCalculationService.calculateAndPublishRating(1L);

        ArgumentCaptor<RatingChangedEvent> eventCaptor = ArgumentCaptor.forClass(RatingChangedEvent.class);
        verify(eventPublisherService).publishRatingChanged(eventCaptor.capture());

        RatingChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAverageRating()).isEqualTo(4.0);
        assertThat(event.getReviewCount()).isEqualTo(1L);
    }

    private ReviewDocument createReview(int rating) {
        return ReviewDocument.builder()
                .stallId(1L)
                .userId("user1")
                .rating(rating)
                .build();
    }
}
