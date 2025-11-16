package com.nushungry.reviewservice.controller;

import com.nushungry.reviewservice.common.ApiResponse;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.dto.ReportResponse;
import com.nushungry.reviewservice.dto.ReportStatistics;
import com.nushungry.reviewservice.service.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员举报管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "管理员举报管理", description = "管理员举报管理相关接口")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminReportController {

    private final ReviewReportService reportService;

    /**
     * 获取待处理的举报列表
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待处理举报", description = "获取待处理和处理中的举报列表")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingReports(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ReportResponse> reportsPage = reportService.getPendingReports(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", reportsPage.getContent());
            response.put("currentPage", reportsPage.getNumber());
            response.put("totalItems", reportsPage.getTotalElements());
            response.put("totalPages", reportsPage.getTotalPages());
            response.put("pageSize", reportsPage.getSize());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting pending reports: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取待处理举报失败: " + e.getMessage()));
        }
    }

    /**
     * 获取所有举报列表
     */
    @GetMapping
    @Operation(summary = "获取所有举报", description = "获取所有举报记录（含已处理）")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllReports(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ReportResponse> reportsPage = reportService.getAllReports(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("content", reportsPage.getContent());
            response.put("currentPage", reportsPage.getNumber());
            response.put("totalItems", reportsPage.getTotalElements());
            response.put("totalPages", reportsPage.getTotalPages());
            response.put("pageSize", reportsPage.getSize());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting all reports: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取所有举报失败: " + e.getMessage()));
        }
    }

    /**
     * 处理举报
     */
    @PutMapping("/{id}")
    @Operation(summary = "处理举报", description = "管理员处理举报")
    public ResponseEntity<ApiResponse<ReportResponse>> handleReport(
            @Parameter(description = "举报ID") @PathVariable String id,
            @Parameter(description = "处理请求") @RequestBody HandleReportRequest request) {

        try {
            // TODO: 从JWT token中获取管理员用户ID
            String adminUserId = "admin"; // 暂时硬编码，实际应该从JWT中解析

            ReportResponse report = reportService.handleReport(id, request, adminUserId);

            log.info("Report {} handled successfully by admin {}", id, adminUserId);
            return ResponseEntity.ok(ApiResponse.success(report));
        } catch (Exception e) {
            log.error("Error handling report: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("处理举报失败: " + e.getMessage()));
        }
    }

    /**
     * 获取举报统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取举报统计", description = "获取举报数据统计")
    public ResponseEntity<ApiResponse<ReportStatistics>> getReportStatistics() {
        try {
            ReportStatistics stats = reportService.getStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting report statistics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取举报统计失败: " + e.getMessage()));
        }
    }

    /**
     * 获取某评价的所有举报
     */
    @GetMapping("/review/{reviewId}")
    @Operation(summary = "获取评价的举报记录", description = "获取某个评价的所有举报记录")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportsByReview(
            @Parameter(description = "评价ID") @PathVariable String reviewId) {

        try {
            List<ReportResponse> reports = reportService.getReportsByReviewId(reviewId);

            Map<String, Object> response = new HashMap<>();
            response.put("reports", reports);
            response.put("total", reports.size());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting reports by review: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取评价举报记录失败: " + e.getMessage()));
        }
    }
}