package com.nushungry.controller;

import com.nushungry.dto.CreateReviewRequest;
import com.nushungry.dto.ReviewResponse;
import com.nushungry.dto.UpdateReviewRequest;
import com.nushungry.dto.CreateReportRequest;
import com.nushungry.dto.ReportResponse;
import com.nushungry.model.Review;
import com.nushungry.model.User;
import com.nushungry.service.ReviewService;
import com.nushungry.service.ReviewLikeService;
import com.nushungry.service.ReviewReportService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 * 提供公开评价查询接口、用户评价管理接口和管理员专用接口
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Review Management", description = "评价管理接口")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewLikeService reviewLikeService;
    private final ReviewReportService reviewReportService;

    // ============ 公开接口（无需认证） ============

    /**
     * 获取评价详情（公开接口）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取评价详情", description = "根据ID获取评价详情")
    public ResponseEntity<Map<String, Object>> getReview(
            @Parameter(description = "评价ID") @PathVariable Long id,
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
     * 获取档口的评价列表（公开接口）
     */
    @GetMapping("/stall/{stallId}")
    @Operation(summary = "获取档口评价", description = "获取指定档口的所有评价（支持分页和排序）")
    public ResponseEntity<Map<String, Object>> getStallReviews(
            @Parameter(description = "档口ID") @PathVariable Long stallId,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序方式 (createdAt, likesCount)") @RequestParam(required = false) String sortBy,
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
                // 分页返回（支持排序）
                Pageable pageable = PageRequest.of(page, size);
                Page<ReviewResponse> reviewsPage;

                if (sortBy != null && !sortBy.isEmpty()) {
                    reviewsPage = reviewService.getReviewsByStallIdWithSort(stallId, pageable, sortBy, currentUserId);
                } else {
                    reviewsPage = reviewService.getReviewsByStallId(stallId, pageable, currentUserId);
                }

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
     * 获取档口的评分分布（公开接口）
     */
    @GetMapping("/stall/{stallId}/rating-distribution")
    @Operation(summary = "获取评分分布", description = "获取档口的评分分布统计")
    public ResponseEntity<Map<String, Object>> getRatingDistribution(
            @Parameter(description = "档口ID") @PathVariable Long stallId) {
        try {
            Map<String, Object> distribution = reviewService.getRatingDistribution(stallId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", distribution);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting rating distribution: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取用户的评价历史（公开接口）
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户评价历史", description = "获取指定用户的所有评价（支持分页）")
    public ResponseEntity<Map<String, Object>> getUserReviews(
            @Parameter(description = "用户ID") @PathVariable Long userId,
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

    // ============ 用户评价管理接口（需要认证） ============

    /**
     * 创建评价（需要认证）
     */
    @PostMapping
    @Operation(summary = "创建评价", description = "用户为档口创建新评价（需要登录）")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User currentUser) {

        try {
            log.info("用户 {} 创建评价", currentUser.getId());
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
     * 更新评价（需要认证，仅限评价作者）
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新评价", description = "更新用户的评价内容（需要登录）")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> updateReview(
            @Parameter(description = "评价ID") @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal User currentUser) {

        try {
            log.info("用户 {} 更新评价 {}", currentUser.getId(), id);
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
     * 删除评价（普通用户删除自己的评价，管理员可删除任意评价）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评价", description = "删除评价（普通用户仅能删除自己的评价，管理员可删除任意评价）")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @Parameter(description = "评价ID") @PathVariable Long id,
            Authentication authentication) {

        try {
            User currentUser = (User) authentication.getPrincipal();
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            log.info("用户 {} (管理员: {}) 删除评价 {}", currentUser.getId(), isAdmin, id);

            if (isAdmin) {
                // 管理员可以删除任意评价
                reviewService.deleteReviewByAdmin(id);
            } else {
                // 普通用户只能删除自己的评价
                reviewService.deleteReview(id, currentUser.getId());
            }

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
     * 点赞评价（切换点赞状态）
     */
    @PostMapping("/{id}/like")
    @Operation(summary = "点赞评价", description = "切换评价的点赞状态（已点赞则取消，未点赞则点赞）")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @Parameter(description = "评价ID") @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        try {
            log.info("用户 {} 切换评价 {} 的点赞状态", currentUser.getId(), id);
            boolean liked = reviewLikeService.toggleLike(id, currentUser.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", liked ? "点赞成功" : "取消点赞成功");
            response.put("liked", liked);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error toggling like: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 举报评价
     */
    @PostMapping("/{id}/report")
    @Operation(summary = "举报评价", description = "举报不当评价内容")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> reportReview(
            @Parameter(description = "评价ID") @PathVariable Long id,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal User currentUser) {

        try {
            log.info("用户 {} 举报评价 {}", currentUser.getId(), id);
            ReportResponse report = reviewReportService.createReport(id, currentUser.getId(), request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "举报提交成功，我们会尽快处理");
            response.put("data", report);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error reporting review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ============ 管理员专用接口（需要 ADMIN 权限） ============

    /**
     * 获取所有评价列表（管理员视图）
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "获取所有评价列表 [管理员]", description = "分页获取所有评价，支持关键词和评分筛选（需要管理员权限）")
    public ResponseEntity<Page<Review>> getAllReviewsForAdmin(
            @Parameter(description = "页码(从0开始)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键词筛选") @RequestParam(required = false) String keyword,
            @Parameter(description = "评分筛选") @RequestParam(required = false) Integer rating,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向(asc/desc)") @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("管理员查询评价列表: page={}, size={}, keyword={}, rating={}", page, size, keyword, rating);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Review> reviews = reviewService.getAllReviewsForAdmin(pageable, keyword, rating);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 获取评价统计信息（管理员专用）
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "获取评价统计 [管理员]", description = "获取评价统计信息（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> getReviewStatsForAdmin() {
        log.info("管理员查询评价统计信息");
        Map<String, Object> stats = reviewService.getReviewStatsForAdmin();
        return ResponseEntity.ok(stats);
    }
}
