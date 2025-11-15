package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.RatingDistributionResponse;
import com.nushungry.reviewservice.event.RatingChangedEvent;
import com.nushungry.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingCalculationService {

    private final ReviewRepository reviewRepository;
    private final EventPublisherService eventPublisherService;

    public void calculateAndPublishRating(Long stallId) {
        log.info("Calculating average rating for stall ID: {}", stallId);
        
        List<ReviewDocument> reviews = reviewRepository.findByStallId(stallId);
        
        if (reviews.isEmpty()) {
            publishRatingEvent(stallId, 0.0, 0L);
            return;
        }

        double averageRating = reviews.stream()
                .mapToInt(ReviewDocument::getRating)
                .average()
                .orElse(0.0);

        publishRatingEvent(stallId, averageRating, (long) reviews.size());
    }

    public RatingDistributionResponse getRatingDistribution(Long stallId) {
        log.info("Getting rating distribution for stall ID: {}", stallId);
        
        List<ReviewDocument> reviews = reviewRepository.findByStallId(stallId);
        
        Map<Integer, Long> distribution = reviews.stream()
                .collect(Collectors.groupingBy(
                        ReviewDocument::getRating,
                        Collectors.counting()
                ));

        // Ensure all ratings 1-5 are present in the distribution
        for (int i = 1; i <= 5; i++) {
            distribution.putIfAbsent(i, 0L);
        }

        double averageRating = reviews.stream()
                .mapToInt(ReviewDocument::getRating)
                .average()
                .orElse(0.0);

        return RatingDistributionResponse.builder()
                .stallId(stallId)
                .averageRating(averageRating)
                .totalReviews((long) reviews.size())
                .distribution(distribution)
                .build();
    }

    private void publishRatingEvent(Long stallId, Double averageRating, Long reviewCount) {
        RatingChangedEvent event = RatingChangedEvent.builder()
                .stallId(stallId)
                .newAverageRating(averageRating)
                .reviewCount(reviewCount)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisherService.publishRatingChanged(event);
    }
}
