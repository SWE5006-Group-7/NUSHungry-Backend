package com.nushungry.controller;

import com.nushungry.dto.HandleReportRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员举报管理控制器
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReviewReportService reviewReportService;

    /**
     * 获取举报列表（支持按状态筛选）
     */
    @GetMapping
    public ResponseEntity<Page<ReviewReport>> getReports(
            @RequestParam(required = false) ReviewReport.ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReviewReport> reports;
        if (status != null) {
            reports = reviewReportService.getReportsByStatus(status, pageable);
        } else {
            reports = reviewReportService.getPendingReports(pageable);
        }

        return ResponseEntity.ok(reports);
    }

    /**
     * 处理举报
     */
    @PostMapping("/{reportId}/handle")
    public ResponseEntity<ReviewReport> handleReport(
            @PathVariable Long reportId,
            @Valid @RequestBody HandleReportRequest request
    ) {
        Long adminId = getCurrentUserId();
        ReviewReport report = reviewReportService.handleReport(reportId, adminId, request);
        return ResponseEntity.ok(report);
    }

    /**
     * 获取举报统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getReportStats() {
        Map<String, Long> stats = reviewReportService.getReportStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取某评价的所有举报记录
     */
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<Page<ReviewReport>> getReportsByReview(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewReport> reports = reviewReportService.getReportsByReviewId(reviewId, pageable);
        return ResponseEntity.ok(reports);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无法获取用户ID");
        }
    }
}
