package com.nushungry.controller;

import com.nushungry.dto.BatchModerationRequest;
import com.nushungry.dto.ModerationRequest;
import com.nushungry.model.ModerationLog;
import com.nushungry.model.Review;
import com.nushungry.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "管理员评价审核", description = "管理员评价内容审核相关接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ModerationService moderationService;

    @GetMapping("/pending")
    @Operation(summary = "获取待审核评价列表", description = "分页获取所有待审核的评价")
    public ResponseEntity<Page<Review>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Review> reviews = moderationService.getPendingReviews(pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取审核统计", description = "获取所有审核状态的评价统计信息")
    public ResponseEntity<Map<String, Long>> getModerationStats() {
        Map<String, Long> stats = moderationService.getModerationStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{reviewId}/moderate")
    @Operation(summary = "审核单个评价", description = "通过或驳回单个评价")
    public ResponseEntity<Review> moderateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ModerationRequest request
    ) {
        Review review = moderationService.moderateReview(reviewId, request);
        return ResponseEntity.ok(review);
    }

    @PostMapping("/moderate/batch")
    @Operation(summary = "批量审核评价", description = "批量通过或驳回多个评价")
    public ResponseEntity<Map<String, Object>> batchModerateReviews(
            @Valid @RequestBody BatchModerationRequest request
    ) {
        Map<String, Object> result = moderationService.batchModerateReviews(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/logs")
    @Operation(summary = "获取审核日志", description = "分页获取所有审核操作日志")
    public ResponseEntity<Page<ModerationLog>> getModerationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ModerationLog> logs = moderationService.getModerationLogs(pageable);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/{reviewId}/logs")
    @Operation(summary = "获取评价的审核日志", description = "获取指定评价的所有审核日志")
    public ResponseEntity<List<ModerationLog>> getReviewModerationLogs(
            @PathVariable Long reviewId
    ) {
        List<ModerationLog> logs = moderationService.getModerationLogsByReviewId(reviewId);
        return ResponseEntity.ok(logs);
    }
}
