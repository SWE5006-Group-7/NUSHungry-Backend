package com.nushungry.reviewservice.controller;

import com.nushungry.reviewservice.common.ApiResponse;
import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.ReviewStatsResponse;
import com.nushungry.reviewservice.repository.ReviewRepository;
import com.nushungry.reviewservice.service.ReviewService;
import com.nushungry.reviewservice.service.RatingCalculationService;
import com.nushungry.reviewservice.service.PriceCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "管理员评价管理", description = "管理员评价管理相关接口")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
public class AdminReviewController {

    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final RatingCalculationService ratingCalculationService;
    private final PriceCalculationService priceCalculationService;
    private final MongoTemplate mongoTemplate;

    @GetMapping
    @Operation(summary = "分页查询所有评价", description = "管理员分页查询所有评价，支持关键词、评分筛选")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String stallId,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("Admin getting reviews with filters - page: {}, size: {}, keyword: {}, rating: {}, stallId: {}, userId: {}",
                page, size, keyword, rating, stallId, userId);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ReviewDocument> reviews;

        if (keyword != null || rating != null || stallId != null || userId != null) {
            reviews = getReviewsWithFilters(keyword, rating, stallId, userId, pageable);
        } else {
            reviews = reviewRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviews.getContent());
        response.put("currentPage", reviews.getNumber());
        response.put("totalItems", reviews.getTotalElements());
        response.put("totalPages", reviews.getTotalPages());
        response.put("pageSize", reviews.getSize());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取评价统计信息", description = "获取评价统计信息，包括总数、平均评分、各星级分布等")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getReviewStats() {
        log.info("Admin getting review statistics");

        // 基础统计
        long totalReviews = reviewRepository.count();

        // 评分统计
        List<ReviewDocument> allReviews = reviewRepository.findAll();
        Double averageRating = 0.0;
        Map<Integer, Long> ratingDistribution = new HashMap<>();

        if (!allReviews.isEmpty()) {
            averageRating = allReviews.stream()
                    .mapToInt(ReviewDocument::getRating)
                    .average()
                    .orElse(0.0);

            ratingDistribution = allReviews.stream()
                    .collect(Collectors.groupingBy(
                            ReviewDocument::getRating,
                            Collectors.counting()
                    ));
        }

        // 填充缺失的星级（1-5星）
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.putIfAbsent(i, 0L);
        }

        // 时间统计
        LocalDateTime now = LocalDateTime.now();
        long todayCount = reviewRepository.countByCreatedAtBetween(
                now.toLocalDate().atStartOfDay(),
                now
        );
        long thisWeekCount = reviewRepository.countByCreatedAtBetween(
                now.minusDays(7),
                now
        );
        long thisMonthCount = reviewRepository.countByCreatedAtBetween(
                now.minusDays(30),
                now
        );

        ReviewStatsResponse stats = ReviewStatsResponse.builder()
                .totalReviews(totalReviews)
                .averageRating(averageRating)
                .ratingDistribution(ratingDistribution)
                .todayCount(todayCount)
                .thisWeekCount(thisWeekCount)
                .thisMonthCount(thisMonthCount)
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "删除评价", description = "管理员删除指定评价")
    public ResponseEntity<ApiResponse<String>> deleteReview(
            @PathVariable String reviewId
    ) {
        log.info("Admin deleting review ID: {}", reviewId);

        ReviewDocument review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found: " + reviewId));

        Long stallId = review.getStallId();

        reviewRepository.deleteById(reviewId);
        log.info("Review deleted successfully by admin");

        // 重新计算评分和价格
        ratingCalculationService.calculateAndPublishRating(stallId);
        priceCalculationService.calculateAndPublishPrice(stallId);

        return ResponseEntity.ok(ApiResponse.success("评价删除成功"));
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除评价", description = "管理员批量删除评价")
    public ResponseEntity<ApiResponse<String>> batchDeleteReviews(
            @RequestBody List<String> reviewIds
    ) {
        log.info("Admin batch deleting {} reviews", reviewIds.size());

        List<Long> affectedStallIds = new ArrayList<>();

        for (String reviewId : reviewIds) {
            ReviewDocument review = reviewRepository.findById(reviewId)
                    .orElse(null);

            if (review != null) {
                affectedStallIds.add(review.getStallId());
                reviewRepository.deleteById(reviewId);
            }
        }

        log.info("Batch delete completed, {} reviews deleted", reviewIds.size());

        // 重新计算受影响摊位的评分和价格
        Set<Long> uniqueStallIds = new HashSet<>(affectedStallIds);
        for (Long stallId : uniqueStallIds) {
            ratingCalculationService.calculateAndPublishRating(stallId);
            priceCalculationService.calculateAndPublishPrice(stallId);
        }

        return ResponseEntity.ok(ApiResponse.success("批量删除评价成功"));
    }

    private Page<ReviewDocument> getReviewsWithFilters(String keyword, Integer rating,
                                                      String stallId, String userId, Pageable pageable) {
        Query query = new Query();

        if (keyword != null && !keyword.trim().isEmpty()) {
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("stallName").regex(keyword.trim(), "i"),
                    Criteria.where("username").regex(keyword.trim(), "i"),
                    Criteria.where("comment").regex(keyword.trim(), "i")
            );
            query.addCriteria(keywordCriteria);
        }

        if (rating != null && rating >= 1 && rating <= 5) {
            query.addCriteria(Criteria.where("rating").is(rating));
        }

        if (stallId != null && !stallId.trim().isEmpty()) {
            try {
                query.addCriteria(Criteria.where("stallId").is(Long.parseLong(stallId)));
            } catch (NumberFormatException e) {
                log.warn("Invalid stallId format: {}", stallId);
            }
        }

        if (userId != null && !userId.trim().isEmpty()) {
            query.addCriteria(Criteria.where("userId").is(userId));
        }

        // 添加排序
        String sortBy = pageable.getSort().stream()
                .findFirst()
                .map(order -> order.getProperty())
                .orElse("createdAt");

        Sort.Direction direction = pageable.getSort().stream()
                .findFirst()
                .map(order -> order.getDirection())
                .orElse(Sort.Direction.DESC);

        query.with(Sort.by(direction, sortBy));

        // 执行查询
        long total = mongoTemplate.count(query, ReviewDocument.class);
        query.skip((long) pageable.getPageNumber() * pageable.getPageSize());
        query.limit(pageable.getPageSize());

        List<ReviewDocument> reviews = mongoTemplate.find(query, ReviewDocument.class);

        return new org.springframework.data.domain.PageImpl<>(reviews, pageable, total);
    }
}