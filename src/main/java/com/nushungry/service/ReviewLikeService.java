package com.nushungry.service;

import com.nushungry.model.Review;
import com.nushungry.model.ReviewLike;
import com.nushungry.model.User;
import com.nushungry.repository.ReviewLikeRepository;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评价点赞服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /**
     * 点赞评价
     */
    @Transactional
    public void likeReview(Long reviewId, Long userId) {
        // 检查是否已点赞
        if (reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            throw new RuntimeException("您已经点赞过此评价");
        }

        // 检查评价是否存在
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        // 检查用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 创建点赞记录
        ReviewLike like = new ReviewLike();
        like.setReview(review);
        like.setUser(user);
        reviewLikeRepository.save(like);

        // 更新评价的点赞数
        review.setLikeCount(review.getLikeCount() + 1);
        reviewRepository.save(review);

        log.info("用户 {} 点赞了评价 {}", userId, reviewId);
    }

    /**
     * 取消点赞
     */
    @Transactional
    public void unlikeReview(Long reviewId, Long userId) {
        // 检查是否已点赞
        if (!reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            throw new RuntimeException("您还未点赞此评价");
        }

        // 删除点赞记录
        reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);

        // 更新评价的点赞数
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
        reviewRepository.save(review);

        log.info("用户 {} 取消点赞评价 {}", userId, reviewId);
    }

    /**
     * 检查用户是否已点赞某评价
     */
    public boolean hasUserLiked(Long reviewId, Long userId) {
        return reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId);
    }

    /**
     * 获取用户点赞过的所有评价ID列表
     */
    public List<Long> getUserLikedReviewIds(Long userId) {
        return reviewLikeRepository.findReviewIdsByUserId(userId);
    }

    /**
     * 获取评价的点赞数
     */
    public long getLikeCount(Long reviewId) {
        return reviewLikeRepository.countByReviewId(reviewId);
    }
}
