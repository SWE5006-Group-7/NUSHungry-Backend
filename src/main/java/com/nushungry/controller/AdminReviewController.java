package com.nushungry.controller;

import com.nushungry.model.Review;
import com.nushungry.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "管理员评价管理", description = "管理员评价管理相关接口")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "获取所有评价列表", description = "分页获取所有评价,支持关键词和评分筛选")
    public ResponseEntity<Page<Review>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Review> reviews = reviewService.getAllReviewsForAdmin(pageable, keyword, rating);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取评价统计", description = "获取评价统计信息")
    public ResponseEntity<Map<String, Object>> getReviewStats() {
        Map<String, Object> stats = reviewService.getReviewStatsForAdmin();
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "删除评价", description = "管理员删除指定评价")
    public ResponseEntity<Map<String, String>> deleteReview(
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReviewByAdmin(reviewId);
        return ResponseEntity.ok(Map.of("message", "评价删除成功"));
    }
}
