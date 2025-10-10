package com.nushungry.service;

import com.nushungry.dto.CreateReviewRequest;
import com.nushungry.dto.ReviewResponse;
import com.nushungry.dto.UpdateReviewRequest;
import com.nushungry.model.Review;
import com.nushungry.model.Stall;
import com.nushungry.model.User;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.StallRepository;
import com.nushungry.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价服务
 */
@Slf4j
@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingCalculationService ratingCalculationService;

    /**
     * 创建评价
     */
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, Long userId) {
        log.info("Creating review for stall {} by user {}", request.getStallId(), userId);

        // 验证摊位是否存在
        Stall stall = stallRepository.findById(request.getStallId())
                .orElseThrow(() -> new RuntimeException("摊位不存在"));

        // 验证用户是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户是否已经评价过该摊位
        if (reviewRepository.existsByUserIdAndStallId(userId, request.getStallId())) {
            throw new RuntimeException("您已经评价过该摊位");
        }

        // 创建评价
        Review review = new Review();
        review.setStall(stall);
        review.setUser(user);
        review.setAuthor(user.getUsername()); // 保持向后兼容
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setImageUrls(request.getImageUrls());

        // 设置多维度评分（如果提供）
        review.setTasteRating(request.getTasteRating());
        review.setEnvironmentRating(request.getEnvironmentRating());
        review.setServiceRating(request.getServiceRating());
        review.setValueRating(request.getValueRating());

        // 如果提供了多维度评分，自动计算平均值作为总体评分
        if (request.getTasteRating() != null || request.getEnvironmentRating() != null ||
            request.getServiceRating() != null || request.getValueRating() != null) {
            double sum = 0;
            int count = 0;
            if (request.getTasteRating() != null) { sum += request.getTasteRating(); count++; }
            if (request.getEnvironmentRating() != null) { sum += request.getEnvironmentRating(); count++; }
            if (request.getServiceRating() != null) { sum += request.getServiceRating(); count++; }
            if (request.getValueRating() != null) { sum += request.getValueRating(); count++; }
            if (count > 0) {
                review.setRating(sum / count);
            }
        }

        review = reviewRepository.save(review);

        // 重新计算摊位评分
        ratingCalculationService.recalculateStallRating(request.getStallId());

        log.info("Review created successfully: {}", review.getId());
        return convertToResponse(review, userId);
    }

    /**
     * 更新评价
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, Long userId) {
        log.info("Updating review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        // 验证权限：只有评价的作者才能编辑
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("您没有权限编辑此评价");
        }

        // 更新评价内容
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        if (request.getImageUrls() != null) {
            review.setImageUrls(request.getImageUrls());
        }

        // 更新多维度评分（如果提供）
        review.setTasteRating(request.getTasteRating());
        review.setEnvironmentRating(request.getEnvironmentRating());
        review.setServiceRating(request.getServiceRating());
        review.setValueRating(request.getValueRating());

        // 如果提供了多维度评分，自动计算平均值作为总体评分
        if (request.getTasteRating() != null || request.getEnvironmentRating() != null ||
            request.getServiceRating() != null || request.getValueRating() != null) {
            double sum = 0;
            int count = 0;
            if (request.getTasteRating() != null) { sum += request.getTasteRating(); count++; }
            if (request.getEnvironmentRating() != null) { sum += request.getEnvironmentRating(); count++; }
            if (request.getServiceRating() != null) { sum += request.getServiceRating(); count++; }
            if (request.getValueRating() != null) { sum += request.getValueRating(); count++; }
            if (count > 0) {
                review.setRating(sum / count);
            }
        }

        review = reviewRepository.save(review);

        // 重新计算摊位评分
        ratingCalculationService.recalculateStallRating(review.getStall().getId());

        log.info("Review updated successfully: {}", reviewId);
        return convertToResponse(review, userId);
    }

    /**
     * 删除评价
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        log.info("Deleting review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        // 验证权限：只有评价的作者才能删除
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("您没有权限删除此评价");
        }

        Long stallId = review.getStall().getId();
        reviewRepository.delete(review);

        // 重新计算摊位评分
        ratingCalculationService.recalculateStallRating(stallId);

        log.info("Review deleted successfully: {}", reviewId);
    }

    /**
     * 获取评价详情
     */
    public ReviewResponse getReviewById(Long reviewId, Long currentUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        return convertToResponse(review, currentUserId);
    }

    /**
     * 获取摊位的评价列表
     */
    public List<ReviewResponse> getReviewsByStallId(Long stallId, Long currentUserId) {
        List<Review> reviews = reviewRepository.findByStallIdOrderByCreatedAtDesc(stallId);
        return reviews.stream()
                .map(review -> convertToResponse(review, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 获取摊位的评价列表（分页）
     */
    public Page<ReviewResponse> getReviewsByStallId(Long stallId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findByStallIdOrderByCreatedAtDesc(stallId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 获取用户的评价历史
     */
    public List<ReviewResponse> getReviewsByUserId(Long userId, Long currentUserId) {
        List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return reviews.stream()
                .map(review -> convertToResponse(review, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的评价历史（分页）
     */
    public Page<ReviewResponse> getReviewsByUserId(Long userId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 获取热门评价（基于综合算法）
     */
    public Page<ReviewResponse> getHotReviews(Long stallId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findHotReviewsByStallId(stallId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 按评分筛选评价
     */
    public Page<ReviewResponse> getReviewsByRating(Long stallId, Double minRating, Double maxRating,
                                                    Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findByStallIdAndRatingBetween(stallId, minRating, maxRating, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 获取高分评价
     */
    public Page<ReviewResponse> getHighRatedReviews(Long stallId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findHighRatedReviewsByStallId(stallId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 获取低分评价
     */
    public Page<ReviewResponse> getLowRatedReviews(Long stallId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findLowRatedReviewsByStallId(stallId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 获取有图评价
     */
    public Page<ReviewResponse> getReviewsWithImages(Long stallId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findReviewsWithImagesByStallId(stallId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 按点赞数排序
     */
    public Page<ReviewResponse> getReviewsByLikes(Long stallId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findByStallIdOrderByLikeCountDesc(stallId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 按评分排序
     */
    public Page<ReviewResponse> getReviewsByRating(Long stallId, Pageable pageable, Long currentUserId) {
        Page<Review> reviews = reviewRepository.findByStallIdOrderByRatingDesc(stallId, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 转换为响应 DTO
     */
    private ReviewResponse convertToResponse(Review review, Long currentUserId) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setStallId(review.getStall().getId());
        response.setStallName(review.getStall().getName());
        response.setUserId(review.getUser().getId());
        response.setUsername(review.getUser().getUsername());
        response.setUserAvatarUrl(review.getUser().getAvatarUrl());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setImageUrls(review.getImageUrls());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        // 多维度评分
        response.setTasteRating(review.getTasteRating());
        response.setEnvironmentRating(review.getEnvironmentRating());
        response.setServiceRating(review.getServiceRating());
        response.setValueRating(review.getValueRating());

        // 点赞数
        response.setLikeCount(review.getLikeCount());

        // 权限判断
        boolean isOwner = currentUserId != null && review.getUser().getId().equals(currentUserId);
        response.setCanEdit(isOwner);
        response.setCanDelete(isOwner);

        return response;
    }
}