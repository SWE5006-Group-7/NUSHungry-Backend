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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @Autowired
    private PriceCalculationService priceCalculationService;

    @Autowired
    private ReviewLikeService reviewLikeService;

    @PersistenceContext
    private EntityManager entityManager;

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
        review.setTotalCost(request.getTotalCost());
        review.setNumberOfPeople(request.getNumberOfPeople());

        review = reviewRepository.save(review);

        // 重新计算摊位评分
        ratingCalculationService.recalculateStallRating(request.getStallId());
        
        // 重新计算摊位人均价格
        priceCalculationService.recalculateStallAveragePrice(request.getStallId());

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
        review.setTotalCost(request.getTotalCost());
        review.setNumberOfPeople(request.getNumberOfPeople());

        review = reviewRepository.save(review);

        // 重新计算摊位评分
        ratingCalculationService.recalculateStallRating(review.getStall().getId());
        
        // 重新计算摊位人均价格
        priceCalculationService.recalculateStallAveragePrice(review.getStall().getId());

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
        
        // 重新计算摊位人均价格
        priceCalculationService.recalculateStallAveragePrice(stallId);

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
        response.setTotalCost(review.getTotalCost());
        response.setNumberOfPeople(review.getNumberOfPeople());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        // 点赞信息
        response.setLikesCount(review.getLikesCount() != null ? review.getLikesCount() : 0);
        if (currentUserId != null) {
            try {
                response.setLiked(reviewLikeService.isLikedByUser(review.getId(), currentUserId));
            } catch (Exception e) {
                response.setLiked(false);
            }
        } else {
            response.setLiked(false);
        }

        // 权限判断
        boolean isOwner = currentUserId != null && review.getUser().getId().equals(currentUserId);
        response.setCanEdit(isOwner);
        response.setCanDelete(isOwner);

        return response;
    }

    /**
     * 管理员获取所有评价(支持筛选)
     */
    public Page<Review> getAllReviewsForAdmin(Pageable pageable, String keyword, Integer rating) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (rating != null) {
                return reviewRepository.findByCommentContainingAndRating(keyword, rating.doubleValue(), pageable);
            } else {
                return reviewRepository.findByCommentContaining(keyword, pageable);
            }
        } else if (rating != null) {
            return reviewRepository.findByRating(rating.doubleValue(), pageable);
        } else {
            return reviewRepository.findAll(pageable);
        }
    }

    /**
     * 获取评价统计信息(管理员用)
     */
    public java.util.Map<String, Object> getReviewStatsForAdmin() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 总评价数
        long totalCount = reviewRepository.count();
        stats.put("totalCount", totalCount);

        // 今日新增(需要ReviewRepository添加方法,暂时返回0)
        stats.put("todayCount", 0L);

        // 平均评分
        Double avgRating = reviewRepository.findAverageRating();
        stats.put("avgRating", avgRating != null ? avgRating : 0.0);

        // 被举报数(需要关联查询,暂时返回0)
        stats.put("reportedCount", 0L);

        return stats;
    }

    /**
     * 获取摊位的评分分布
     */
    public java.util.Map<String, Object> getRatingDistribution(Long stallId) {
        List<Object[]> distribution = reviewRepository.getRatingDistributionByStallId(stallId);

        // 初始化评分分布(1-5星)
        java.util.Map<Integer, Long> ratingCount = new java.util.HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingCount.put(i, 0L);
        }

        // 填充实际数据
        for (Object[] row : distribution) {
            Double rating = (Double) row[0];
            Long count = (Long) row[1];
            ratingCount.put(rating.intValue(), count);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("distribution", ratingCount);
        result.put("total", reviewRepository.countByStallId(stallId));
        result.put("average", reviewRepository.getAverageRatingByStallId(stallId));

        return result;
    }

    /**
     * 获取摊位的评价列表（支持排序）
     */
    public Page<ReviewResponse> getReviewsByStallIdWithSort(Long stallId, Pageable pageable, String sortBy, Long currentUserId) {
        Page<Review> reviews;

        if (sortBy != null && (sortBy.equals("likesCount") || sortBy.equals("createdAt"))) {
            reviews = reviewRepository.findByStallIdWithSort(stallId, sortBy, pageable);
        } else {
            // 默认按创建时间排序
            reviews = reviewRepository.findByStallIdOrderByCreatedAtDesc(stallId, pageable);
        }

        return reviews.map(review -> convertToResponse(review, currentUserId));
    }

    /**
     * 管理员删除评价
     */
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        log.info("Admin deleting review {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        Long stallId = review.getStall().getId();
        
        // 先删除所有相关表的记录，避免外键约束问题
        try {
            // 1. 删除 moderation_log
            int deletedLogs = entityManager.createNativeQuery(
                "DELETE FROM moderation_log WHERE review_id = :reviewId")
                .setParameter("reviewId", reviewId)
                .executeUpdate();
            log.info("Deleted {} moderation_log entries for review {}", deletedLogs, reviewId);
            
            // 2. 删除 review_reports (举报记录)
            int deletedReports = entityManager.createNativeQuery(
                "DELETE FROM review_reports WHERE review_id = :reviewId")
                .setParameter("reviewId", reviewId)
                .executeUpdate();
            log.info("Deleted {} review_reports entries for review {}", deletedReports, reviewId);
            
            // 3. 删除 review_likes (点赞记录)
            int deletedLikes = entityManager.createNativeQuery(
                "DELETE FROM review_likes WHERE review_id = :reviewId")
                .setParameter("reviewId", reviewId)
                .executeUpdate();
            log.info("Deleted {} review_likes entries for review {}", deletedLikes, reviewId);
            
            // 4. 删除 review_images (评价图片关联)
            int deletedImages = entityManager.createNativeQuery(
                "DELETE FROM review_images WHERE review_id = :reviewId")
                .setParameter("reviewId", reviewId)
                .executeUpdate();
            log.info("Deleted {} review_images entries for review {}", deletedImages, reviewId);
            
        } catch (Exception e) {
            log.error("Failed to delete related records for review {}: {}", reviewId, e.getMessage());
            throw new RuntimeException("删除评价相关记录失败: " + e.getMessage());
        }
        
        // 删除评价
        reviewRepository.delete(review);

        // 重新计算摊位评分
        ratingCalculationService.recalculateStallRating(stallId);
        
        // 重新计算摊位人均价格
        priceCalculationService.recalculateStallAveragePrice(stallId);

        log.info("Review deleted by admin successfully: {}", reviewId);
    }
}
