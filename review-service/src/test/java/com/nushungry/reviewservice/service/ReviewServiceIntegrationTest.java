package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.CreateReviewRequest;
import com.nushungry.reviewservice.dto.ReviewResponse;
import com.nushungry.reviewservice.dto.UpdateReviewRequest;
import com.nushungry.reviewservice.event.PriceChangedEvent;
import com.nushungry.reviewservice.event.RatingChangedEvent;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.UnauthorizedException;
import com.nushungry.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
class ReviewServiceIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RatingCalculationService ratingCalculationService;

    @Autowired
    private ReviewRepository reviewRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private static final Long STALL_ID = 1L;
    private static final String USER_ID_1 = "user1";
    private static final String USER_ID_2 = "user2";
    private static final String USERNAME = "Test User";
    private static final String AVATAR_URL = "http://example.com/avatar.jpg";

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
    }


    @Test
    void testCreateReview_CompleteWorkflow() {
        // Given
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(5)
                .comment("Great food!")
                .imageUrls(Arrays.asList("img1.jpg", "img2.jpg"))
                .totalCost(20.0)
                .numberOfPeople(2)
                .build();

        // When
        ReviewResponse response = reviewService.createReview(request, USER_ID_1, USERNAME, AVATAR_URL);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(5, response.getRating());
        assertEquals("Great food!", response.getComment());
        assertEquals(STALL_ID, response.getStallId());

        // Verify rating event was published
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(),
                anyString(),
                any(RatingChangedEvent.class)
        );

        // Verify price event was published
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(),
                anyString(),
                any(PriceChangedEvent.class)
        );
    }


    @Test
    void testUpdateReview_RecalculatesRating() {
        // Given - create initial review
        CreateReviewRequest createRequest = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(3)
                .comment("Average")
                .build();

        ReviewResponse created = reviewService.createReview(createRequest, USER_ID_1, USERNAME, AVATAR_URL);
        reset(rabbitTemplate); // Reset mock after creation

        // When - update rating
        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                .rating(5)
                .comment("Much better now!")
                .build();

        ReviewResponse updated = reviewService.updateReview(created.getId(), updateRequest, USER_ID_1);

        // Then
        assertEquals(5, updated.getRating());
        assertEquals("Much better now!", updated.getComment());

        // Verify rating was recalculated and event published
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(),
                anyString(),
                any(RatingChangedEvent.class)
        );
    }


    @Test
    void testDeleteReview_RecalculatesRating() {
        // Given - create review
        CreateReviewRequest createRequest = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(4)
                .comment("Good")
                .build();

        ReviewResponse created = reviewService.createReview(createRequest, USER_ID_1, USERNAME, AVATAR_URL);
        reset(rabbitTemplate);

        // When - delete review
        reviewService.deleteReview(created.getId(), USER_ID_1);

        // Then - verify review is deleted
        assertFalse(reviewRepository.existsById(created.getId()));

        // Verify rating was recalculated (should be 0 now)
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(
                anyString(),
                anyString(),
                any(RatingChangedEvent.class)
        );
    }


    @Test
    void testRatingCalculation_Accuracy() {
        // Given - create multiple reviews with different ratings
        createReview(STALL_ID, 5, USER_ID_1);
        createReview(STALL_ID, 4, USER_ID_2);
        createReview(STALL_ID, 3, "user3");

        // When
        List<ReviewDocument> reviews = reviewRepository.findByStallId(STALL_ID);
        double averageRating = reviews.stream()
                .mapToInt(ReviewDocument::getRating)
                .average()
                .orElse(0.0);

        // Then
        assertEquals(3, reviews.size());
        assertEquals(4.0, averageRating, 0.01); // (5+4+3)/3 = 4.0
    }


    @Test
    void testConcurrentReviewCreation_DataConsistency() throws InterruptedException {
        // Given
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - create reviews concurrently
        for (int i = 0; i < threadCount; i++) {
            final String userId = "user" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    createReview(STALL_ID, 4, userId);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        endLatch.await(10, TimeUnit.SECONDS); // Wait for completion
        executor.shutdown();

        // Then - verify all reviews were created
        List<ReviewDocument> reviews = reviewRepository.findByStallId(STALL_ID);
        assertEquals(threadCount, reviews.size(), "All concurrent reviews should be created");

        // Verify each user has exactly one review
        for (int i = 0; i < threadCount; i++) {
            String userId = "user" + i;
            long count = reviews.stream()
                    .filter(r -> r.getUserId().equals(userId))
                    .count();
            assertEquals(1, count, "User " + userId + " should have exactly one review");
        }
    }


    @Test
    void testGetReviewsByStallId_PaginationAndSorting() {
        // Given - create multiple reviews
        createReview(STALL_ID, 5, USER_ID_1);
        createReview(STALL_ID, 4, USER_ID_2);
        createReview(STALL_ID, 3, "user3");

        // When - query first page
        Pageable pageable = PageRequest.of(0, 2);
        Page<ReviewResponse> page = reviewService.getReviewsByStallId(STALL_ID, "createdAt", USER_ID_1, pageable);

        // Then
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }


    @Test
    void testUpdateReview_Unauthorized() {
        // Given - create review by user1
        CreateReviewRequest createRequest = CreateReviewRequest.builder()
                .stallId(STALL_ID)
                .stallName("Test Stall")
                .rating(4)
                .comment("Good")
                .build();

        ReviewResponse created = reviewService.createReview(createRequest, USER_ID_1, USERNAME, AVATAR_URL);

        // When/Then - user2 tries to update user1's review
        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                .rating(1)
                .comment("Bad")
                .build();

        assertThrows(UnauthorizedException.class, () -> {
            reviewService.updateReview(created.getId(), updateRequest, USER_ID_2);
        });
    }


    @Test
    void testDeleteReview_NotFound() {
        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.deleteReview("nonexistent-id", USER_ID_1);
        });
    }


    @Test
    void testRatingDistribution() {
        // Given - create reviews with different ratings
        createReview(STALL_ID, 5, USER_ID_1);
        createReview(STALL_ID, 5, USER_ID_2);
        createReview(STALL_ID, 4, "user3");
        createReview(STALL_ID, 3, "user4");
        createReview(STALL_ID, 3, "user5");

        // When
        var distribution = ratingCalculationService.getRatingDistribution(STALL_ID);

        // Then
        assertEquals(5, distribution.getTotalReviews());
        assertEquals(2L, distribution.getDistribution().get(5));
        assertEquals(1L, distribution.getDistribution().get(4));
        assertEquals(2L, distribution.getDistribution().get(3));
        assertEquals(0L, distribution.getDistribution().get(2));
        assertEquals(0L, distribution.getDistribution().get(1));
    }

    // Helper method to create a review
    private void createReview(Long stallId, int rating, String userId) {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(stallId)
                .stallName("Test Stall")
                .rating(rating)
                .comment("Test comment")
                .build();
        reviewService.createReview(request, userId, USERNAME, AVATAR_URL);
    }
}
