package com.nushungry.reviewservice.performance;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.CreateReviewRequest;
import com.nushungry.reviewservice.dto.ReviewResponse;
import com.nushungry.reviewservice.repository.ReviewLikeRepository;
import com.nushungry.reviewservice.repository.ReviewRepository;
import com.nushungry.reviewservice.service.ReviewService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and stress testing for Review Service
 * 
 * Tests include:
 * - Large dataset query performance
 * - Pagination performance under load
 * - MongoDB index effectiveness
 * - Concurrent operations handling
 * - Memory and resource management
 */
@SpringBootTest
@ActiveProfiles("test")
class PerformanceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private static final Long STALL_ID = 100L;
    private static final String USERNAME = "Test User";
    private static final String AVATAR_URL = "http://example.com/avatar.jpg";
    private Random random = new Random();

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        reviewLikeRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        reviewLikeRepository.deleteAll();
    }


    /**
     * Test 1: Large dataset query performance
     * Creates 1000 reviews and measures query time
     */
    @Test
    void testLargeDatasetQueryPerformance() {
        // Given - create 1000 reviews
        int reviewCount = 1000;
        System.out.println("Creating " + reviewCount + " reviews for performance test...");
        
        long startCreate = System.currentTimeMillis();
        for (int i = 0; i < reviewCount; i++) {
            createReview(STALL_ID, randomRating(), "user" + i);
        }
        long endCreate = System.currentTimeMillis();
        System.out.println("Created " + reviewCount + " reviews in " + (endCreate - startCreate) + "ms");

        // When - query all reviews (should use index)
        long startQuery = System.currentTimeMillis();
        List<ReviewDocument> reviews = reviewRepository.findByStallId(STALL_ID);
        long endQuery = System.currentTimeMillis();
        long queryTime = endQuery - startQuery;

        // Then - verify results and performance
        assertEquals(reviewCount, reviews.size());
        System.out.println("Queried " + reviewCount + " reviews in " + queryTime + "ms");
        
        // Query should complete within 1 second for 1000 records with proper indexing
        assertTrue(queryTime < 1000, 
            "Query took " + queryTime + "ms, should be < 1000ms (index may be missing)");
    }


    /**
     * Test 2: Pagination performance under load
     * Tests pagination efficiency with large dataset
     */
    @Test
    void testPaginationPerformance() {
        // Given - create 500 reviews
        int reviewCount = 500;
        System.out.println("Creating " + reviewCount + " reviews for pagination test...");
        
        for (int i = 0; i < reviewCount; i++) {
            createReview(STALL_ID, randomRating(), "user" + i);
        }

        // When - query multiple pages
        int pageSize = 20;
        int totalPages = reviewCount / pageSize;
        long totalQueryTime = 0;
        List<Long> pageTimes = new ArrayList<>();

        System.out.println("Testing pagination with " + totalPages + " pages...");
        for (int page = 0; page < totalPages; page++) {
            Pageable pageable = PageRequest.of(page, pageSize);
            
            long startPage = System.currentTimeMillis();
            Page<ReviewResponse> result = reviewService.getReviewsByStallId(
                STALL_ID, "createdAt", "test-user", pageable);
            long endPage = System.currentTimeMillis();
            long pageTime = endPage - startPage;
            
            totalQueryTime += pageTime;
            pageTimes.add(pageTime);
            
            assertEquals(pageSize, result.getContent().size(), 
                "Page " + page + " should have " + pageSize + " items");
        }

        // Then - analyze performance
        double avgPageTime = totalQueryTime / (double) totalPages;
        long maxPageTime = pageTimes.stream().max(Long::compareTo).orElse(0L);
        long minPageTime = pageTimes.stream().min(Long::compareTo).orElse(0L);

        System.out.println("Pagination Performance:");
        System.out.println("  Total pages: " + totalPages);
        System.out.println("  Average page query time: " + String.format("%.2f", avgPageTime) + "ms");
        System.out.println("  Min page time: " + minPageTime + "ms");
        System.out.println("  Max page time: " + maxPageTime + "ms");

        // Each page query should complete within 500ms
        assertTrue(avgPageTime < 500, 
            "Average page time " + avgPageTime + "ms exceeds 500ms threshold");
        
        // Verify pagination consistency (last page time shouldn't be significantly slower)
        double timeVariance = (maxPageTime - minPageTime) / avgPageTime;
        assertTrue(timeVariance < 2.0, 
            "Page time variance too high: " + String.format("%.2f", timeVariance) + 
            " (may indicate missing index)");
    }


    /**
     * Test 3: Index effectiveness test
     * Compares query performance with different sort orders
     */
    @Test
    void testIndexEffectiveness() {
        // Given - create reviews with varying likes count
        int reviewCount = 500;
        System.out.println("Creating " + reviewCount + " reviews for index test...");
        
        List<String> reviewIds = new ArrayList<>();
        for (int i = 0; i < reviewCount; i++) {
            ReviewResponse review = createReview(STALL_ID, randomRating(), "user" + i);
            reviewIds.add(review.getId());
            
            // Randomly set likes count to test sorting by likes
            ReviewDocument doc = reviewRepository.findById(review.getId()).orElseThrow();
            doc.setLikesCount(random.nextInt(100));
            reviewRepository.save(doc);
        }

        Pageable pageable = PageRequest.of(0, 50);

        // Test 1: Sort by createdAt (should have index)
        long startCreatedAt = System.currentTimeMillis();
        Page<ReviewResponse> byCreatedAt = reviewService.getReviewsByStallId(
            STALL_ID, "createdAt", "test-user", pageable);
        long timeCreatedAt = System.currentTimeMillis() - startCreatedAt;

        // Test 2: Sort by likesCount (should have index)
        long startLikes = System.currentTimeMillis();
        Page<ReviewResponse> byLikes = reviewService.getReviewsByStallId(
            STALL_ID, "likesCount", "test-user", pageable);
        long timeLikes = System.currentTimeMillis() - startLikes;

        // Then - verify both queries are fast
        System.out.println("Index Effectiveness:");
        System.out.println("  Sort by createdAt: " + timeCreatedAt + "ms");
        System.out.println("  Sort by likesCount: " + timeLikes + "ms");

        assertEquals(50, byCreatedAt.getContent().size());
        assertEquals(50, byLikes.getContent().size());
        
        // Both queries should be fast with proper indexes
        assertTrue(timeCreatedAt < 500, "Sort by createdAt took " + timeCreatedAt + "ms (should be < 500ms)");
        assertTrue(timeLikes < 500, "Sort by likesCount took " + timeLikes + "ms (should be < 500ms)");
        
        // Index-based queries should have similar performance
        double performanceRatio = Math.max(timeCreatedAt, timeLikes) / 
                                  (double) Math.min(timeCreatedAt, timeLikes);
        assertTrue(performanceRatio < 3.0, 
            "Performance difference too large: " + String.format("%.2f", performanceRatio) + "x");
    }


    /**
     * Test 4: Concurrent read/write stress test
     * Simulates high concurrent load
     */
    @Test
    void testConcurrentReadWriteStress() throws InterruptedException {
        // Given - prepare initial data
        int initialReviews = 100;
        System.out.println("Preparing " + initialReviews + " initial reviews...");
        
        for (int i = 0; i < initialReviews; i++) {
            createReview(STALL_ID, randomRating(), "initial-user" + i);
        }

        // When - simulate concurrent operations
        int threadCount = 20;
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successfulWrites = new AtomicInteger(0);
        AtomicInteger successfulReads = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        System.out.println("Starting stress test: " + threadCount + " threads, " + 
                         operationsPerThread + " operations each...");
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int op = 0; op < operationsPerThread; op++) {
                        try {
                            if (random.nextBoolean()) {
                                // Write operation
                                createReview(STALL_ID, randomRating(), "thread" + threadId + "-op" + op);
                                successfulWrites.incrementAndGet();
                            } else {
                                // Read operation
                                Pageable pageable = PageRequest.of(0, 20);
                                reviewService.getReviewsByStallId(STALL_ID, "createdAt", 
                                    "thread" + threadId, pageable);
                                successfulReads.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errors.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then - verify results
        assertTrue(completed, "Stress test should complete within 30 seconds");
        
        int totalOperations = threadCount * operationsPerThread;
        double throughput = (totalOperations / (totalTime / 1000.0));

        System.out.println("Stress Test Results:");
        System.out.println("  Total time: " + totalTime + "ms");
        System.out.println("  Successful writes: " + successfulWrites.get());
        System.out.println("  Successful reads: " + successfulReads.get());
        System.out.println("  Errors: " + errors.get());
        System.out.println("  Throughput: " + String.format("%.2f", throughput) + " ops/sec");

        // Verify data consistency
        long finalCount = reviewRepository.countByStallId(STALL_ID);
        long expectedCount = initialReviews + successfulWrites.get();
        assertEquals(expectedCount, finalCount, "Review count should match writes");
        
        // Error rate should be low
        double errorRate = errors.get() / (double) totalOperations;
        assertTrue(errorRate < 0.05, 
            "Error rate too high: " + String.format("%.2f%%", errorRate * 100));
    }


    /**
     * Test 5: Memory efficiency test
     * Ensures pagination doesn't load all data into memory
     */
    @Test
    void testMemoryEfficiency() {
        // Given - create large dataset
        int reviewCount = 1000;
        System.out.println("Creating " + reviewCount + " reviews for memory test...");
        
        for (int i = 0; i < reviewCount; i++) {
            createReview(STALL_ID, randomRating(), "user" + i);
        }

        // Force garbage collection to get baseline
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // When - query multiple pages (should not load all data)
        int pageSize = 20;
        int pagesToQuery = 10;
        
        for (int page = 0; page < pagesToQuery; page++) {
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<ReviewResponse> result = reviewService.getReviewsByStallId(
                STALL_ID, "createdAt", "test-user", pageable);
            
            // Process page (simulate real usage)
            result.getContent().forEach(review -> {
                assertNotNull(review.getId());
            });
        }

        // Then - check memory usage
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        System.out.println("Memory Efficiency Test:");
        System.out.println("  Memory before: " + (memoryBefore / 1024 / 1024) + " MB");
        System.out.println("  Memory after: " + (memoryAfter / 1024 / 1024) + " MB");
        System.out.println("  Memory used: " + (memoryUsed / 1024 / 1024) + " MB");

        // Memory usage should be reasonable (not loading all 1000 reviews)
        // Allow 50MB for pagination operations
        assertTrue(memoryUsed < 50 * 1024 * 1024, 
            "Memory usage too high: " + (memoryUsed / 1024 / 1024) + " MB (may be loading entire dataset)");
    }


    /**
     * Test 6: Bulk operation performance
     * Tests performance of bulk inserts
     */
    @Test
    void testBulkOperationPerformance() {
        // Given
        int batchSize = 100;
        int batchCount = 10;
        
        System.out.println("Testing bulk operations: " + batchCount + " batches of " + batchSize + " reviews...");
        
        long totalTime = 0;
        List<Long> batchTimes = new ArrayList<>();

        // When - insert multiple batches
        for (int batch = 0; batch < batchCount; batch++) {
            long batchStart = System.currentTimeMillis();
            
            for (int i = 0; i < batchSize; i++) {
                createReview(STALL_ID, randomRating(), "batch" + batch + "-user" + i);
            }
            
            long batchTime = System.currentTimeMillis() - batchStart;
            batchTimes.add(batchTime);
            totalTime += batchTime;
        }

        // Then - analyze performance
        double avgBatchTime = totalTime / (double) batchCount;
        double insertsPerSecond = (batchSize * batchCount) / (totalTime / 1000.0);

        System.out.println("Bulk Operation Performance:");
        System.out.println("  Total reviews: " + (batchSize * batchCount));
        System.out.println("  Total time: " + totalTime + "ms");
        System.out.println("  Average batch time: " + String.format("%.2f", avgBatchTime) + "ms");
        System.out.println("  Inserts per second: " + String.format("%.2f", insertsPerSecond));

        // Verify all reviews were created
        long finalCount = reviewRepository.countByStallId(STALL_ID);
        assertEquals(batchSize * batchCount, finalCount);
        
        // Should achieve reasonable throughput
        assertTrue(insertsPerSecond > 10, 
            "Insert throughput too low: " + String.format("%.2f", insertsPerSecond) + " inserts/sec");
    }


    // Helper methods
    
    private ReviewResponse createReview(Long stallId, int rating, String userId) {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .stallId(stallId)
                .stallName("Test Stall")
                .rating(rating)
                .comment("Test comment for performance testing")
                .totalCost(10.0 + random.nextDouble() * 20.0)
                .numberOfPeople(1 + random.nextInt(4))
                .build();
        
        return reviewService.createReview(request, userId, USERNAME, AVATAR_URL);
    }

    private int randomRating() {
        return 1 + random.nextInt(5); // Random rating between 1 and 5
    }
}
