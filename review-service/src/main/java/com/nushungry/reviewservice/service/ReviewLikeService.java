package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.document.ReviewLikeDocument;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.repository.ReviewLikeRepository;
import com.nushungry.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public boolean toggleLike(String reviewId, String userId) {
        log.info("Toggling like for review ID: {} by user: {}", reviewId, userId);

        ReviewDocument review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        boolean exists = reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);

        if (exists) {
            reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);
            review.setLikesCount(Math.max(0, review.getLikesCount() - 1));
            reviewRepository.save(review);
            log.info("Like removed for review ID: {}", reviewId);
            return false;
        } else {
            ReviewLikeDocument like = ReviewLikeDocument.builder()
                    .reviewId(reviewId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            reviewLikeRepository.save(like);
            
            review.setLikesCount(review.getLikesCount() + 1);
            reviewRepository.save(review);
            log.info("Like added for review ID: {}", reviewId);
            return true;
        }
    }

    public boolean isLikedByUser(String reviewId, String userId) {
        return reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);
    }

    public long getLikeCount(String reviewId) {
        return reviewLikeRepository.countByReviewId(reviewId);
    }
}
