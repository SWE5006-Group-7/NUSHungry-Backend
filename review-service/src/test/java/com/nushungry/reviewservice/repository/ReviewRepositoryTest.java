package com.nushungry.reviewservice.repository;

import com.nushungry.reviewservice.document.ReviewDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    @Test
    void testSaveAndFindById() {
        ReviewDocument review = createReview(1L, "user1", 5, "Excellent!");
        
        ReviewDocument saved = reviewRepository.save(review);
        
        assertThat(saved.getId()).isNotNull();
        Optional<ReviewDocument> found = reviewRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStallId()).isEqualTo(1L);
        assertThat(found.get().getUserId()).isEqualTo("user1");
    }

    @Test
    void testFindByStallIdOrderByCreatedAtDesc() {
        ReviewDocument review1 = createReview(1L, "user1", 5, "Great!");
        review1.setCreatedAt(LocalDateTime.now().minusDays(2));
        reviewRepository.save(review1);

        ReviewDocument review2 = createReview(1L, "user2", 4, "Good");
        review2.setCreatedAt(LocalDateTime.now().minusDays(1));
        reviewRepository.save(review2);

        ReviewDocument review3 = createReview(1L, "user3", 3, "OK");
        review3.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review3);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewDocument> page = reviewRepository.findByStallIdOrderByCreatedAtDesc(1L, pageable);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getComment()).isEqualTo("OK");
        assertThat(page.getContent().get(1).getComment()).isEqualTo("Good");
        assertThat(page.getContent().get(2).getComment()).isEqualTo("Great!");
    }

    @Test
    void testFindByStallIdOrderByLikesCountDesc() {
        ReviewDocument review1 = createReview(1L, "user1", 5, "Review 1");
        review1.setLikesCount(10);
        reviewRepository.save(review1);

        ReviewDocument review2 = createReview(1L, "user2", 4, "Review 2");
        review2.setLikesCount(25);
        reviewRepository.save(review2);

        ReviewDocument review3 = createReview(1L, "user3", 3, "Review 3");
        review3.setLikesCount(5);
        reviewRepository.save(review3);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewDocument> page = reviewRepository.findByStallIdOrderByLikesCountDesc(1L, pageable);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getLikesCount()).isEqualTo(25);
        assertThat(page.getContent().get(1).getLikesCount()).isEqualTo(10);
        assertThat(page.getContent().get(2).getLikesCount()).isEqualTo(5);
    }

    @Test
    void testFindByUserId() {
        reviewRepository.save(createReview(1L, "user1", 5, "Review 1"));
        reviewRepository.save(createReview(2L, "user1", 4, "Review 2"));
        reviewRepository.save(createReview(3L, "user2", 3, "Review 3"));

        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewDocument> page = reviewRepository.findByUserIdOrderByCreatedAtDesc("user1", pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
            .extracting(ReviewDocument::getUserId)
            .containsOnly("user1");
    }

    @Test
    void testCountByStallId() {
        reviewRepository.save(createReview(1L, "user1", 5, "Review 1"));
        reviewRepository.save(createReview(1L, "user2", 4, "Review 2"));
        reviewRepository.save(createReview(2L, "user3", 3, "Review 3"));

        long count = reviewRepository.countByStallId(1L);
        
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testExistsByUserIdAndStallId() {
        reviewRepository.save(createReview(1L, "user1", 5, "Review 1"));

        boolean exists = reviewRepository.existsByUserIdAndStallId("user1", 1L);
        assertThat(exists).isTrue();

        boolean notExists = reviewRepository.existsByUserIdAndStallId("user1", 2L);
        assertThat(notExists).isFalse();
    }

    @Test
    void testFindByStallId() {
        reviewRepository.save(createReview(1L, "user1", 5, "Review 1"));
        reviewRepository.save(createReview(1L, "user2", 4, "Review 2"));
        reviewRepository.save(createReview(2L, "user3", 3, "Review 3"));

        List<ReviewDocument> reviews = reviewRepository.findByStallId(1L);

        assertThat(reviews).hasSize(2);
        assertThat(reviews)
            .extracting(ReviewDocument::getStallId)
            .containsOnly(1L);
    }

    @Test
    void testPaginationWithMultiplePages() {
        for (int i = 0; i < 25; i++) {
            reviewRepository.save(createReview(1L, "user" + i, 5, "Review " + i));
        }

        Pageable page1 = PageRequest.of(0, 10);
        Page<ReviewDocument> firstPage = reviewRepository.findByStallIdOrderByCreatedAtDesc(1L, page1);
        assertThat(firstPage.getContent()).hasSize(10);
        assertThat(firstPage.getTotalElements()).isEqualTo(25);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);

        Pageable page2 = PageRequest.of(1, 10);
        Page<ReviewDocument> secondPage = reviewRepository.findByStallIdOrderByCreatedAtDesc(1L, page2);
        assertThat(secondPage.getContent()).hasSize(10);

        Pageable page3 = PageRequest.of(2, 10);
        Page<ReviewDocument> thirdPage = reviewRepository.findByStallIdOrderByCreatedAtDesc(1L, page3);
        assertThat(thirdPage.getContent()).hasSize(5);
    }

    @Test
    void testDeleteReview() {
        ReviewDocument review = createReview(1L, "user1", 5, "To be deleted");
        ReviewDocument saved = reviewRepository.save(review);

        reviewRepository.deleteById(saved.getId());

        Optional<ReviewDocument> deleted = reviewRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();
    }

    private ReviewDocument createReview(Long stallId, String userId, int rating, String comment) {
        ReviewDocument review = new ReviewDocument();
        review.setStallId(stallId);
        review.setStallName("Stall " + stallId);
        review.setUserId(userId);
        review.setUsername("User " + userId);
        review.setRating(rating);
        review.setComment(comment);
        review.setImageUrls(Arrays.asList("image1.jpg", "image2.jpg"));
        review.setTotalCost(20.0);
        review.setNumberOfPeople(2);
        review.setLikesCount(0);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return review;
    }
}
