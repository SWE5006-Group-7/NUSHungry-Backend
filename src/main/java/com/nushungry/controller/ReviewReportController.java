package com.nushungry.controller;

import com.nushungry.dto.CreateReportRequest;
import com.nushungry.model.ReviewReport;
import com.nushungry.service.ReviewReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 评价举报控制器（用户使用）
 */
@RestController
@RequestMapping("/api/reviews/{reviewId}/reports")
@RequiredArgsConstructor
@Slf4j
public class ReviewReportController {

    private final ReviewReportService reviewReportService;

    /**
     * 创建举报
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createReport(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        Long userId = getCurrentUserId();

        // 检查是否已举报
        if (reviewReportService.hasUserReported(reviewId, userId)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "您已经举报过此评价");
            return ResponseEntity.badRequest().body(error);
        }

        ReviewReport report = reviewReportService.createReport(reviewId, userId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "举报提交成功，我们会尽快处理");
        response.put("reportId", report.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * 检查当前用户是否已举报某评价
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkReportStatus(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        boolean reported = reviewReportService.hasUserReported(reviewId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("reported", reported);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取某评价的举报统计（仅返回举报次数）
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getReportCount(@PathVariable Long reviewId) {
        Pageable pageable = PageRequest.of(0, 1);
        Page<ReviewReport> reports = reviewReportService.getReportsByReviewId(reviewId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("reportCount", reports.getTotalElements());

        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        String username = authentication.getName();
        // 假设username就是userId（根据实际情况调整）
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无法获取用户ID");
        }
    }
}
