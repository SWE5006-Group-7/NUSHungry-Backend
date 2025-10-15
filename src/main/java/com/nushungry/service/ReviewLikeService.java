package com.nushungry.service;

import com.nushungry.model.Review;
import com.nushungry.model.ReviewLike;
import com.nushungry.model.User;
import com.nushungry.repository.ReviewLikeRepository;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价点赞服务
 */
@Service
public class ReviewLikeService {

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 点赞评价
     */
    @Transactional
    public void likeReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查是否已经点赞
        if (reviewLikeRepository.existsByReviewAndUser(review, user)) {
            throw new RuntimeException("您已经点赞过此评价");
        }

        // 创建点赞记录
        ReviewLike like = new ReviewLike();
        like.setReview(review);
        like.setUser(user);
        reviewLikeRepository.save(like);

        // 更新评价的点赞计数
        review.setLikesCount(review.getLikesCount() + 1);
        reviewRepository.save(review);
    }

    /**
     * 取消点赞
     */
    @Transactional
    public void unlikeReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查是否已点赞
        if (!reviewLikeRepository.existsByReviewAndUser(review, user)) {
            throw new RuntimeException("您还未点赞此评价");
        }

        // 删除点赞记录
        reviewLikeRepository.deleteByReviewAndUser(review, user);

        // 更新评价的点赞计数
        if (review.getLikesCount() > 0) {
            review.setLikesCount(review.getLikesCount() - 1);
            reviewRepository.save(review);
        }
    }

    /**
     * 切换点赞状态（已点赞则取消，未点赞则点赞）
     */
    @Transactional
    public boolean toggleLike(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        boolean isLiked = reviewLikeRepository.existsByReviewAndUser(review, user);

        if (isLiked) {
            // 取消点赞
            reviewLikeRepository.deleteByReviewAndUser(review, user);
            if (review.getLikesCount() > 0) {
                review.setLikesCount(review.getLikesCount() - 1);
            }
            reviewRepository.save(review);
            return false; // 返回false表示已取消点赞
        } else {
            // 点赞
            ReviewLike like = new ReviewLike();
            like.setReview(review);
            like.setUser(user);
            reviewLikeRepository.save(like);

            review.setLikesCount(review.getLikesCount() + 1);
            reviewRepository.save(review);
            return true; // 返回true表示已点赞
        }
    }

    /**
     * 检查用户是否已点赞某评价
     */
    public boolean isLikedByUser(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return reviewLikeRepository.existsByReviewAndUser(review, user);
    }

    /**
     * 批量检查用户对多个评价的点赞状态
     */
    public Map<Long, Boolean> checkLikedStatus(List<Long> reviewIds, Long userId) {
        List<Long> likedReviewIds = reviewLikeRepository.findLikedReviewIdsByUserAndReviewIds(userId, reviewIds);

        return reviewIds.stream()
                .collect(Collectors.toMap(
                    reviewId -> reviewId,
                    likedReviewIds::contains
                ));
    }

    /**
     * 获取评价的点赞数
     */
    public long getLikesCount(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        return reviewLikeRepository.countByReview(review);
    }
}
