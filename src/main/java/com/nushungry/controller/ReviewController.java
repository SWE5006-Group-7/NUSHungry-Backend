package com.nushungry.controller;

import com.nushungry.dto.CreateReviewRequest;
import com.nushungry.dto.ReviewResponse;
import com.nushungry.dto.UpdateReviewRequest;
import com.nushungry.model.User;
import com.nushungry.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 * 提供评价相关的API端点
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Review Management", description = "评价管理接口")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 创建评价
     */
    @PostMapping
    @Operation(summary = "创建评价", description = "用户为摊位创建新评价")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User currentUser) {

        try {
            ReviewResponse review = reviewService.createReview(request, currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "评价创建成功");
            response.put("data", review);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新评价
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新评价", description = "更新用户的评价内容")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal User currentUser) {

        try {
            ReviewResponse review = reviewService.updateReview(id, request, currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "评价更新成功");
            response.put("data", review);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除评价
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评价", description = "删除用户的评价")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        try {
            reviewService.deleteReview(id, currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "评价删除成功");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取评价详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取评价详情", description = "根据ID获取评价详情")
    public ResponseEntity<Map<String, Object>> getReview(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;
            ReviewResponse review = reviewService.getReviewById(id, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", review);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取摊位的评价列表
     */
    @GetMapping("/stall/{stallId}")
    @Operation(summary = "获取摊位评价", description = "获取指定摊位的所有评价")
    public ResponseEntity<Map<String, Object>> getStallReviews(
            @PathVariable Long stallId,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;

            if (size == 0) {
                // 不分页，返回所有评价
                List<ReviewResponse> reviews = reviewService.getReviewsByStallId(stallId, currentUserId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", reviews);
                response.put("total", reviews.size());

                return ResponseEntity.ok(response);
            } else {
                // 分页返回
                Pageable pageable = PageRequest.of(page, size);
                Page<ReviewResponse> reviewsPage = reviewService.getReviewsByStallId(stallId, pageable, currentUserId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", reviewsPage.getContent());
                response.put("currentPage", reviewsPage.getNumber());
                response.put("totalItems", reviewsPage.getTotalElements());
                response.put("totalPages", reviewsPage.getTotalPages());
                response.put("pageSize", reviewsPage.getSize());

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error getting stall reviews: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户的评价历史
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户评价历史", description = "获取指定用户的所有评价")
    public ResponseEntity<Map<String, Object>> getUserReviews(
            @PathVariable Long userId,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;

            if (size == 0) {
                // 不分页，返回所有评价
                List<ReviewResponse> reviews = reviewService.getReviewsByUserId(userId, currentUserId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", reviews);
                response.put("total", reviews.size());

                return ResponseEntity.ok(response);
            } else {
                // 分页返回
                Pageable pageable = PageRequest.of(page, size);
                Page<ReviewResponse> reviewsPage = reviewService.getReviewsByUserId(userId, pageable, currentUserId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", reviewsPage.getContent());
                response.put("currentPage", reviewsPage.getNumber());
                response.put("totalItems", reviewsPage.getTotalElements());
                response.put("totalPages", reviewsPage.getTotalPages());
                response.put("pageSize", reviewsPage.getSize());

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error getting user reviews: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取热门评价（综合算法：点赞数 + 评分 + 时间新鲜度）
     */
    @GetMapping("/stall/{stallId}/hot")
    @Operation(summary = "获取热门评价", description = "获取摊位的热门评价（基于综合算法）")
    public ResponseEntity<Map<String, Object>> getHotReviews(
            @PathVariable Long stallId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> reviewsPage = reviewService.getHotReviews(stallId, pageable, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewsPage.getContent());
            response.put("currentPage", reviewsPage.getNumber());
            response.put("totalItems", reviewsPage.getTotalElements());
            response.put("totalPages", reviewsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting hot reviews: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取高分评价（4星及以上）
     */
    @GetMapping("/stall/{stallId}/high-rated")
    @Operation(summary = "获取高分评价", description = "获取摊位的高分评价（4星及以上）")
    public ResponseEntity<Map<String, Object>> getHighRatedReviews(
            @PathVariable Long stallId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> reviewsPage = reviewService.getHighRatedReviews(stallId, pageable, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewsPage.getContent());
            response.put("currentPage", reviewsPage.getNumber());
            response.put("totalItems", reviewsPage.getTotalElements());
            response.put("totalPages", reviewsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting high-rated reviews: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取低分评价（2星及以下）
     */
    @GetMapping("/stall/{stallId}/low-rated")
    @Operation(summary = "获取低分评价", description = "获取摊位的低分评价（2星及以下）")
    public ResponseEntity<Map<String, Object>> getLowRatedReviews(
            @PathVariable Long stallId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> reviewsPage = reviewService.getLowRatedReviews(stallId, pageable, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewsPage.getContent());
            response.put("currentPage", reviewsPage.getNumber());
            response.put("totalItems", reviewsPage.getTotalElements());
            response.put("totalPages", reviewsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting low-rated reviews: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取有图评价
     */
    @GetMapping("/stall/{stallId}/with-images")
    @Operation(summary = "获取有图评价", description = "获取摊位的带图片评价")
    public ResponseEntity<Map<String, Object>> getReviewsWithImages(
            @PathVariable Long stallId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> reviewsPage = reviewService.getReviewsWithImages(stallId, pageable, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewsPage.getContent());
            response.put("currentPage", reviewsPage.getNumber());
            response.put("totalItems", reviewsPage.getTotalElements());
            response.put("totalPages", reviewsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting reviews with images: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 按点赞数排序获取评价
     */
    @GetMapping("/stall/{stallId}/by-likes")
    @Operation(summary = "按点赞数排序", description = "按点赞数降序获取摊位评价")
    public ResponseEntity<Map<String, Object>> getReviewsByLikes(
            @PathVariable Long stallId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> reviewsPage = reviewService.getReviewsByLikes(stallId, pageable, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewsPage.getContent());
            response.put("currentPage", reviewsPage.getNumber());
            response.put("totalItems", reviewsPage.getTotalElements());
            response.put("totalPages", reviewsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting reviews by likes: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 按评分排序获取评价
     */
    @GetMapping("/stall/{stallId}/by-rating")
    @Operation(summary = "按评分排序", description = "按评分降序获取摊位评价")
    public ResponseEntity<Map<String, Object>> getReviewsByRatingSorted(
            @PathVariable Long stallId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            Long currentUserId = authentication != null ? ((User) authentication.getPrincipal()).getId() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<ReviewResponse> reviewsPage = reviewService.getReviewsByRating(stallId, pageable, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reviewsPage.getContent());
            response.put("currentPage", reviewsPage.getNumber());
            response.put("totalItems", reviewsPage.getTotalElements());
            response.put("totalPages", reviewsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting reviews by rating: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}