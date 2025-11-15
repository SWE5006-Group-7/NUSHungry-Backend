package com.nushungry.reviewservice.repository;

import com.nushungry.reviewservice.document.ReviewLikeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
class ReviewLikeRepositoryTest {

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @BeforeEach
    void setUp() {
        reviewLikeRepository.deleteAll();
    }

    @Test
    void testSaveReviewLike() {
        ReviewLikeDocument like = createLike("review1", "user1");
        
        ReviewLikeDocument saved = reviewLikeRepository.save(like);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getReviewId()).isEqualTo("review1");
        assertThat(saved.getUserId()).isEqualTo("user1");
    }

    @Test
    void testUniqueConstraint() {
        reviewLikeRepository.save(createLike("review1", "user1"));
        
        assertThatThrownBy(() -> {
            reviewLikeRepository.save(createLike("review1", "user1"));
        }).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testExistsByReviewIdAndUserId() {
        reviewLikeRepository.save(createLike("review1", "user1"));

        boolean exists = reviewLikeRepository.existsByReviewIdAndUserId("review1", "user1");
        assertThat(exists).isTrue();

        boolean notExists = reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2");
        assertThat(notExists).isFalse();
    }

    @Test
    void testDeleteByReviewIdAndUserId() {
        reviewLikeRepository.save(createLike("review1", "user1"));
        reviewLikeRepository.save(createLike("review1", "user2"));

        reviewLikeRepository.deleteByReviewIdAndUserId("review1", "user1");

        assertThat(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user1")).isFalse();
        assertThat(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user2")).isTrue();
    }

    @Test
    void testCountByReviewId() {
        reviewLikeRepository.save(createLike("review1", "user1"));
        reviewLikeRepository.save(createLike("review1", "user2"));
        reviewLikeRepository.save(createLike("review1", "user3"));
        reviewLikeRepository.save(createLike("review2", "user1"));

        long count = reviewLikeRepository.countByReviewId("review1");
        
        assertThat(count).isEqualTo(3);
    }

    @Test
    void testFindByReviewId() {
        reviewLikeRepository.save(createLike("review1", "user1"));
        reviewLikeRepository.save(createLike("review1", "user2"));
        reviewLikeRepository.save(createLike("review2", "user3"));

        List<ReviewLikeDocument> likes = reviewLikeRepository.findByReviewId("review1");

        assertThat(likes).hasSize(2);
        assertThat(likes)
            .extracting(ReviewLikeDocument::getReviewId)
            .containsOnly("review1");
        assertThat(likes)
            .extracting(ReviewLikeDocument::getUserId)
            .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void testDeleteAllByReviewId() {
        reviewLikeRepository.save(createLike("review1", "user1"));
        reviewLikeRepository.save(createLike("review1", "user2"));
        reviewLikeRepository.save(createLike("review2", "user3"));

        reviewLikeRepository.deleteByReviewId("review1");

        assertThat(reviewLikeRepository.findByReviewId("review1")).isEmpty();
        assertThat(reviewLikeRepository.findByReviewId("review2")).hasSize(1);
    }

    @Test
    void testMultipleUsersLikingSameReview() {
        reviewLikeRepository.save(createLike("review1", "user1"));
        reviewLikeRepository.save(createLike("review1", "user2"));
        reviewLikeRepository.save(createLike("review1", "user3"));

        long count = reviewLikeRepository.countByReviewId("review1");
        assertThat(count).isEqualTo(3);

        List<ReviewLikeDocument> likes = reviewLikeRepository.findByReviewId("review1");
        assertThat(likes).hasSize(3);
    }

    @Test
    void testSameUserLikingMultipleReviews() {
        reviewLikeRepository.save(createLike("review1", "user1"));
        reviewLikeRepository.save(createLike("review2", "user1"));
        reviewLikeRepository.save(createLike("review3", "user1"));

        assertThat(reviewLikeRepository.existsByReviewIdAndUserId("review1", "user1")).isTrue();
        assertThat(reviewLikeRepository.existsByReviewIdAndUserId("review2", "user1")).isTrue();
        assertThat(reviewLikeRepository.existsByReviewIdAndUserId("review3", "user1")).isTrue();
    }

    private ReviewLikeDocument createLike(String reviewId, String userId) {
        ReviewLikeDocument like = new ReviewLikeDocument();
        like.setReviewId(reviewId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        return like;
    }
}
