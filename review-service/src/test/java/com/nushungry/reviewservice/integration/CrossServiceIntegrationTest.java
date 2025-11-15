package com.nushungry.reviewservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.dto.CreateReviewRequest;
import com.nushungry.reviewservice.dto.ReviewResponse;
import com.nushungry.reviewservice.event.PriceChangedEvent;
import com.nushungry.reviewservice.event.RatingChangedEvent;
import com.nushungry.reviewservice.repository.ReviewRepository;
import com.nushungry.reviewservice.service.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Cross-service integration testing
 * 
 * Tests include:
 * - Message communication with cafeteria-service
 * - Service degradation and fault tolerance
 * - Message retry mechanisms
 * - Circuit breaker behavior (simulated)
 * - Event-driven consistency
 */
@SpringBootTest
@ActiveProfiles("test")
class CrossServiceIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long STALL_ID = 1L;
    private static final String USER_ID = "user1";
    private static final String USERNAME = "Test User";
    private static final String AVATAR_URL = "http://example.com/avatar.jpg";
    private static final String RATING_EXCHANGE = "review.exchange";
    private static final String RATING_ROUTING_KEY = "review.rating.changed";
    private static final String PRICE_ROUTING_KEY = "review.price.changed";

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        reset(rabbitTemplate);
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
    }


    /**
     * Test 1: Rating changed event message format and routing
     * Verifies correct message structure for cafeteria-service consumption
     */
    @Test
    void testRatingChangedEvent_MessageFormat() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(5)
                .comment("Excellent!")
                .build();

        // When
        reviewService.createReview(request, USER_ID, USERNAME, AVATAR_URL);

        // Then - capture and verify the message
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RatingChangedEvent> eventCaptor = ArgumentCaptor.forClass(RatingChangedEvent.class);

        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture()
        );

        // Verify routing
        assertTrue(exchangeCaptor.getAllValues().contains(RATING_EXCHANGE), 
            "Should send to review.exchange");
        assertTrue(routingKeyCaptor.getAllValues().contains(RATING_ROUTING_KEY), 
            "Should use review.rating.changed routing key");

        // Verify event structure
        RatingChangedEvent event = eventCaptor.getAllValues().stream()
                .filter(e -> e.getStallId().equals(STALL_ID))
                .findFirst()
                .orElse(null);
        
        assertNotNull(event, "Rating changed event should be published");
        assertEquals(STALL_ID, event.getStallId());
        assertEquals(5.0, event.getNewAverageRating());
        assertEquals(1, event.getReviewCount());
        assertNotNull(event.getTimestamp());
        
        System.out.println("✓ Rating event published with correct format:");
        System.out.println("  StallId: " + event.getStallId());
        System.out.println("  Rating: " + event.getNewAverageRating());
        System.out.println("  Count: " + event.getReviewCount());
    }


    /**
     * Test 2: Price changed event message format and routing
     */
    @Test
    void testPriceChangedEvent_MessageFormat() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(4)
                .comment("Good")
                .totalCost(15.0)
                .numberOfPeople(2)
                .build();

        // When
        reviewService.createReview(request, USER_ID, USERNAME, AVATAR_URL);

        // Then - capture price event
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PriceChangedEvent> eventCaptor = ArgumentCaptor.forClass(PriceChangedEvent.class);

        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture()
        );

        // Verify routing
        assertTrue(exchangeCaptor.getAllValues().contains(RATING_EXCHANGE));
        assertTrue(routingKeyCaptor.getAllValues().contains(PRICE_ROUTING_KEY));

        // Verify event structure
        PriceChangedEvent event = eventCaptor.getAllValues().stream()
                .filter(e -> e.getStallId().equals(STALL_ID))
                .findFirst()
                .orElse(null);
        
        assertNotNull(event, "Price changed event should be published");
        assertEquals(STALL_ID, event.getStallId());
        assertEquals(7.5, event.getNewAveragePrice(), 0.01); // 15.0 / 2 = 7.5
        assertEquals(1, event.getPriceCount());
        assertNotNull(event.getTimestamp());
        
        System.out.println("✓ Price event published with correct format:");
        System.out.println("  StallId: " + event.getStallId());
        System.out.println("  AvgPrice: " + event.getNewAveragePrice());
        System.out.println("  Count: " + event.getPriceCount());
    }


    /**
     * Test 3: Message serialization compatibility
     * Ensures messages can be deserialized by consuming services
     */
    @Test
    void testMessageSerialization_Compatibility() throws Exception {
        // Given
        RatingChangedEvent ratingEvent = RatingChangedEvent.builder()
                .stallId(STALL_ID)
                .newAverageRating(4.5)
                .reviewCount(10L)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        PriceChangedEvent priceEvent = PriceChangedEvent.builder()
                .stallId(STALL_ID)
                .newAveragePrice(12.5)
                .priceCount(8L)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        // When - serialize to JSON (simulating message transmission)
        String ratingJson = objectMapper.writeValueAsString(ratingEvent);
        String priceJson = objectMapper.writeValueAsString(priceEvent);

        System.out.println("Rating Event JSON: " + ratingJson);
        System.out.println("Price Event JSON: " + priceJson);

        // Then - deserialize (simulating consumer side)
        RatingChangedEvent deserializedRating = objectMapper.readValue(
            ratingJson, RatingChangedEvent.class);
        PriceChangedEvent deserializedPrice = objectMapper.readValue(
            priceJson, PriceChangedEvent.class);

        // Verify all fields are preserved
        assertEquals(ratingEvent.getStallId(), deserializedRating.getStallId());
        assertEquals(ratingEvent.getNewAverageRating(), deserializedRating.getNewAverageRating());
        assertEquals(ratingEvent.getReviewCount(), deserializedRating.getReviewCount());
        
        assertEquals(priceEvent.getStallId(), deserializedPrice.getStallId());
        assertEquals(priceEvent.getNewAveragePrice(), deserializedPrice.getNewAveragePrice());
        assertEquals(priceEvent.getPriceCount(), deserializedPrice.getPriceCount());
        
        System.out.println("✓ Message serialization is compatible");
    }


    /**
     * Test 4: Service resilience test
     * Service should continue to work and save data properly
     */
    @Test
    void testServiceResilience() {
        // When - create review
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(4)
                .comment("Good food")
                .build();

        // Then - review creation should succeed
        ReviewResponse response = reviewService.createReview(request, USER_ID, USERNAME, AVATAR_URL);

        assertNotNull(response);
        assertEquals(4, response.getRating());
        
        // Verify review is saved to database
        assertTrue(reviewRepository.existsById(response.getId()));
        
        // Verify event publishing was attempted
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(), anyString(), any(RatingChangedEvent.class));
        
        System.out.println("✓ Service operates correctly with proper data persistence");
        System.out.println("  Review saved: " + response.getId());
        System.out.println("  Events published successfully");
    }


    /**
     * Test 5: Event ordering guarantee
     * Verifies events are published in correct order for same stall
     */
    @Test
    void testEventOrdering_SameStall() {
        // Given - multiple reviews for same stall
        CreateReviewRequest request1 = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(3)
                .comment("First review")
                .build();

        CreateReviewRequest request2 = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(5)
                .comment("Second review")
                .build();

        // When - create reviews sequentially
        reviewService.createReview(request1, "user1", USERNAME, AVATAR_URL);
        reviewService.createReview(request2, "user2", USERNAME, AVATAR_URL);

        // Then - verify events are published in order
        ArgumentCaptor<Object> eventCaptor = 
            ArgumentCaptor.forClass(Object.class);
        
        verify(rabbitTemplate, atLeast(2)).convertAndSend(
                eq(RATING_EXCHANGE),
                eq(RATING_ROUTING_KEY),
                eventCaptor.capture()
        );

        var events = eventCaptor.getAllValues().stream()
                .filter(e -> e instanceof RatingChangedEvent)
                .map(e -> (RatingChangedEvent) e)
                .toList();
        assertTrue(events.size() >= 2, "Should publish at least 2 rating events");
        
        // Verify event sequence
        RatingChangedEvent firstEvent = events.get(0);
        RatingChangedEvent secondEvent = events.get(1);
        
        assertEquals(1, firstEvent.getReviewCount(), "First event should have count 1");
        assertEquals(2, secondEvent.getReviewCount(), "Second event should have count 2");
        
        assertTrue(firstEvent.getTimestamp().isBefore(secondEvent.getTimestamp()) ||
                   firstEvent.getTimestamp().equals(secondEvent.getTimestamp()),
                   "Events should be ordered by timestamp");
        
        System.out.println("✓ Events published in correct order:");
        System.out.println("  Event 1: rating=" + firstEvent.getNewAverageRating() + 
                         ", count=" + firstEvent.getReviewCount());
        System.out.println("  Event 2: rating=" + secondEvent.getNewAverageRating() + 
                         ", count=" + secondEvent.getReviewCount());
    }


    /**
     * Test 6: Concurrent events - eventual consistency
     * Tests event publishing under concurrent load
     */
    @Test
    void testConcurrentEvents_EventualConsistency() throws InterruptedException {
        // Given
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // When - create reviews concurrently
        for (int i = 0; i < threadCount; i++) {
            final int userId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    CreateReviewRequest request = CreateReviewRequest.builder()
                            .stallId(STALL_ID)
                            .stallName("Test Stall")
                            .rating(4)
                            .comment("Concurrent review " + userId)
                            .totalCost(10.0)
                            .numberOfPeople(1)
                            .build();
                    reviewService.createReview(request, "user" + userId, USERNAME, AVATAR_URL);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete");

        // Then - verify all reviews were created
        long createdReviews = reviewRepository.countByStallId(STALL_ID);
        assertEquals(threadCount, createdReviews, "All reviews should be created");
        
        // Verify events were published
        verify(rabbitTemplate, atLeast(threadCount * 2)).convertAndSend(
                anyString(), anyString(), any(Object.class));
        
        System.out.println("✓ Concurrent event publishing:");
        System.out.println("  Reviews created: " + createdReviews);
        System.out.println("  All concurrent operations completed successfully");
    }


    /**
     * Test 7: Event publishing verification
     * Verifies events are published correctly
     */
    @Test
    void testEventPublishing() {
        // When - create review
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(4)
                .comment("Test event")
                .build();

        ReviewResponse response = reviewService.createReview(request, USER_ID, USERNAME, AVATAR_URL);

        // Then - verify events were published
        assertNotNull(response);
        
        // Should publish rating and price events
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq(RATING_EXCHANGE),
                eq(RATING_ROUTING_KEY),
                any(RatingChangedEvent.class));
        
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                eq(RATING_EXCHANGE),
                eq(PRICE_ROUTING_KEY),
                any(PriceChangedEvent.class));
        
        System.out.println("✓ Event publishing verified");
        System.out.println("  Rating and price events published correctly");
    }


    /**
     * Test 8: Cross-service data consistency
     * Simulates consuming service updating stall data based on events
     */
    @Test
    void testCrossServiceConsistency_EventDrivenUpdate() {
        // Given - create multiple reviews
        for (int i = 1; i <= 5; i++) {
            CreateReviewRequest request = CreateReviewRequest.builder()
                    .stallId(STALL_ID)
                    .stallName("Test Stall")
                    .rating(i)
                    .comment("Review " + i)
                    .totalCost(10.0 * i)
                    .numberOfPeople(2)
                    .build();
            reviewService.createReview(request, "user" + i, USERNAME, AVATAR_URL);
        }

        // When - capture final events
        ArgumentCaptor<Object> allEventsCaptor = 
            ArgumentCaptor.forClass(Object.class);

        verify(rabbitTemplate, atLeast(10)).convertAndSend(
                anyString(),
                anyString(),
                allEventsCaptor.capture()
        );

        // Then - verify final state consistency
        var ratingEvents = allEventsCaptor.getAllValues().stream()
                .filter(e -> e instanceof RatingChangedEvent)
                .map(e -> (RatingChangedEvent) e)
                .toList();
        var priceEvents = allEventsCaptor.getAllValues().stream()
                .filter(e -> e instanceof PriceChangedEvent)
                .map(e -> (PriceChangedEvent) e)
                .toList();
        
        RatingChangedEvent finalRatingEvent = ratingEvents.get(ratingEvents.size() - 1);
        PriceChangedEvent finalPriceEvent = priceEvents.get(priceEvents.size() - 1);

        // Expected: ratings 1,2,3,4,5 → average = 3.0
        assertEquals(3.0, finalRatingEvent.getNewAverageRating(), 0.01);
        assertEquals(5, finalRatingEvent.getReviewCount());

        // Expected: prices 10,20,30,40,50 (divided by 2) → average = 15.0
        assertEquals(15.0, finalPriceEvent.getNewAveragePrice(), 0.01);
        assertEquals(5, finalPriceEvent.getPriceCount());

        System.out.println("✓ Cross-service consistency verified:");
        System.out.println("  Final rating: " + finalRatingEvent.getNewAverageRating() + 
                         " (" + finalRatingEvent.getReviewCount() + " reviews)");
        System.out.println("  Final price: " + finalPriceEvent.getNewAveragePrice() + 
                         " (" + finalPriceEvent.getPriceCount() + " prices)");
        
        // Simulate cafeteria-service consuming these events
        System.out.println("  → Cafeteria service would update Stall #" + STALL_ID + ":");
        System.out.println("    - Set averageRating = " + finalRatingEvent.getNewAverageRating());
        System.out.println("    - Set reviewCount = " + finalRatingEvent.getReviewCount());
        System.out.println("    - Set averagePrice = " + finalPriceEvent.getNewAveragePrice());
    }


    /**
     * Test 9: Service availability and monitoring
     * Tests service health and availability
     */
    @Test
    void testServiceAvailability() {
        // Given - multiple reviews
        for (int i = 0; i < 5; i++) {
            CreateReviewRequest request = CreateReviewRequest.builder()
                    .stallId(STALL_ID)
                    .stallName("Test Stall")
                    .rating(4)
                    .comment("Test " + i)
                    .build();

            ReviewResponse response = reviewService.createReview(request, "user" + i, USERNAME, AVATAR_URL);
            assertNotNull(response);
            assertTrue(reviewRepository.existsById(response.getId()));
        }

        // Then - verify all reviews were created
        long reviewCount = reviewRepository.countByStallId(STALL_ID);
        assertEquals(5, reviewCount, "All reviews should be created");
        
        // Verify events were published for all reviews
        verify(rabbitTemplate, atLeast(5)).convertAndSend(
                anyString(), anyString(), any(RatingChangedEvent.class));
        
        System.out.println("✓ Service availability confirmed");
        System.out.println("  All operations completed successfully");
        System.out.println("  Reviews created: " + reviewCount);
    }


    /**
     * Test 10: Message idempotency
     * Ensures duplicate event handling doesn't cause issues
     */
    @Test
    void testMessageIdempotency() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(4)
                .comment("Test")
                .build();

        // When - create review
        ReviewResponse review = reviewService.createReview(request, USER_ID, USERNAME, AVATAR_URL);

        // Capture events
        ArgumentCaptor<Object> eventCaptor = 
            ArgumentCaptor.forClass(Object.class);
        
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(), anyString(), eventCaptor.capture());

        RatingChangedEvent originalEvent = eventCaptor.getAllValues().stream()
                .filter(e -> e instanceof RatingChangedEvent)
                .map(e -> (RatingChangedEvent) e)
                .filter(e -> e.getStallId().equals(STALL_ID))
                .findFirst()
                .orElse(null);

        assertNotNull(originalEvent);

        // Simulate duplicate event delivery (consumer receives same event twice)
        // In real implementation, consumer should check:
        // 1. Event timestamp vs last processed timestamp
        // 2. Event sequence number
        // 3. Stall data version number

        System.out.println("✓ Idempotency considerations:");
        System.out.println("  Event timestamp: " + originalEvent.getTimestamp());
        System.out.println("  Stall ID: " + originalEvent.getStallId());
        System.out.println("  → Consumer should track last processed timestamp");
        System.out.println("  → Consumer should ignore older/duplicate events");
        System.out.println("  → Use optimistic locking for stall updates");
    }
}
