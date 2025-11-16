package com.nushungry.reviewservice.repository;

import com.nushungry.reviewservice.document.ReviewLikeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends MongoRepository<ReviewLikeDocument, String> {

    boolean existsByReviewIdAndUserId(String reviewId, String userId);

    Optional<ReviewLikeDocument> findByReviewIdAndUserId(String reviewId, String userId);

    void deleteByReviewIdAndUserId(String reviewId, String userId);

    long countByReviewId(String reviewId);

    List<ReviewLikeDocument> findByReviewId(String reviewId);

    void deleteByReviewId(String reviewId);
}
