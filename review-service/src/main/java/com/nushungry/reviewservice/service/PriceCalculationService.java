package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.event.PriceChangedEvent;
import com.nushungry.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceCalculationService {

    private final ReviewRepository reviewRepository;
    private final EventPublisherService eventPublisherService;

    public void calculateAndPublishPrice(Long stallId) {
        log.info("Calculating average price for stall ID: {}", stallId);
        
        List<ReviewDocument> reviews = reviewRepository.findByStallId(stallId);
        
        List<ReviewDocument> validPriceReviews = reviews.stream()
                .filter(r -> r.getTotalCost() != null && r.getTotalCost() > 0)
                .filter(r -> r.getNumberOfPeople() != null && r.getNumberOfPeople() > 0)
                .toList();

        if (validPriceReviews.isEmpty()) {
            publishPriceEvent(stallId, 0.0, 0L);
            return;
        }

        double averagePrice = validPriceReviews.stream()
                .mapToDouble(r -> r.getTotalCost() / r.getNumberOfPeople())
                .average()
                .orElse(0.0);

        publishPriceEvent(stallId, averagePrice, (long) validPriceReviews.size());
    }

    private void publishPriceEvent(Long stallId, Double averagePrice, Long priceCount) {
        PriceChangedEvent event = PriceChangedEvent.builder()
                .stallId(stallId)
                .newAveragePrice(averagePrice)
                .priceCount(priceCount)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisherService.publishPriceChanged(event);
    }
}
