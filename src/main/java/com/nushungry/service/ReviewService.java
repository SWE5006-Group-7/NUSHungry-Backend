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

        // 权限判断
        boolean isOwner = currentUserId != null && review.getUser().getId().equals(currentUserId);
        response.setCanEdit(isOwner);
        response.setCanDelete(isOwner);

        return response;
    }
}