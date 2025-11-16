package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.event.PriceChangedEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceCalculationServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private PriceCalculationService priceCalculationService;

    private List<ReviewDocument> testReviews;

    @BeforeEach
    void setUp() {
        testReviews = Arrays.asList(
                createReview(20.0, 2),  // 10 per person
                createReview(30.0, 3),  // 10 per person
                createReview(40.0, 2),  // 20 per person
                createReview(50.0, 5)   // 10 per person
        );
    }

    @Test
    void testCalculateAndPublishPriceWithReviews() {
        when(reviewRepository.findByStallId(1L)).thenReturn(testReviews);

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getStallId()).isEqualTo(1L);
        assertThat(event.getNewAveragePrice()).isEqualTo(12.5);
        assertThat(event.getPriceCount()).isEqualTo(4L);
    }

    @Test
    void testCalculateAndPublishPriceWithNoReviews() {
        when(reviewRepository.findByStallId(1L)).thenReturn(Collections.emptyList());

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getStallId()).isEqualTo(1L);
        assertThat(event.getNewAveragePrice()).isEqualTo(0.0);
        assertThat(event.getPriceCount()).isEqualTo(0L);
    }

    @Test
    void testCalculatePriceFilteringInvalidData() {
        List<ReviewDocument> mixedReviews = Arrays.asList(
                createReview(20.0, 2),      // Valid: 10 per person
                createReview(null, 2),      // Invalid: null cost
                createReview(0.0, 2),       // Invalid: zero cost
                createReview(20.0, null),   // Invalid: null people
                createReview(20.0, 0),      // Invalid: zero people
                createReview(30.0, 3)       // Valid: 10 per person
        );
        when(reviewRepository.findByStallId(1L)).thenReturn(mixedReviews);

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAveragePrice()).isEqualTo(10.0);
        assertThat(event.getPriceCount()).isEqualTo(2L);
    }

    @Test
    void testCalculatePriceWithAllInvalidData() {
        List<ReviewDocument> invalidReviews = Arrays.asList(
                createReview(null, 2),
                createReview(0.0, 2),
                createReview(20.0, null),
                createReview(20.0, 0)
        );
        when(reviewRepository.findByStallId(1L)).thenReturn(invalidReviews);

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAveragePrice()).isEqualTo(0.0);
        assertThat(event.getPriceCount()).isEqualTo(0L);
    }

    @Test
    void testCalculatePriceWithSingleValidReview() {
        List<ReviewDocument> singleReview = Collections.singletonList(createReview(25.0, 5));
        when(reviewRepository.findByStallId(1L)).thenReturn(singleReview);

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAveragePrice()).isEqualTo(5.0);
        assertThat(event.getPriceCount()).isEqualTo(1L);
    }

    @Test
    void testCalculatePriceWithVaryingPrices() {
        List<ReviewDocument> varyingPrices = Arrays.asList(
                createReview(10.0, 1),   // 10 per person
                createReview(100.0, 1),  // 100 per person
                createReview(30.0, 3)    // 10 per person
        );
        when(reviewRepository.findByStallId(1L)).thenReturn(varyingPrices);

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAveragePrice()).isEqualTo(40.0);
    }

    @Test
    void testCalculatePriceWithDecimalResults() {
        List<ReviewDocument> decimalPrices = Arrays.asList(
                createReview(15.5, 2),   // 7.75 per person
                createReview(22.3, 3)    // 7.433... per person
        );
        when(reviewRepository.findByStallId(1L)).thenReturn(decimalPrices);

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAveragePrice()).isBetween(7.5, 7.7);
    }

    @Test
    void testCalculatePriceWithLargeGroup() {
        List<ReviewDocument> largeGroup = Collections.singletonList(createReview(1000.0, 50));
        when(reviewRepository.findByStallId(1L)).thenReturn(largeGroup);

        priceCalculationService.calculateAndPublishPrice(1L);

        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);
        verify(eventPublisherService).publishPriceChanged(eventCaptor.capture());

        PriceChangedEvent event = eventCaptor.getValue();
        assertThat(event.getNewAveragePrice()).isEqualTo(20.0);
        assertThat(event.getPriceCount()).isEqualTo(1L);
    }

    private ReviewDocument createReview(Double totalCost, Integer numberOfPeople) {
        return ReviewDocument.builder()
                .stallId(1L)
                .userId("user1")
                .totalCost(totalCost)
                .numberOfPeople(numberOfPeople)
                .build();
    }
}
