package com.nushungry.controller;

import com.nushungry.dto.HandleReportRequest;
import com.nushungry.dto.ReportResponse;
import com.nushungry.model.User;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@CrossOrigin(origins = "*")
@Tag(name = "Admin Report Management", description = "管理员举报管理接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReviewReportService reportService;

    /**
     * 获取待处理的举报列表
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待处理举报", description = "获取待处理和处理中的举报列表")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> getPendingReports(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ReportResponse> reportsPage = reportService.getPendingReports(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reportsPage.getContent());
            response.put("currentPage", reportsPage.getNumber());
            response.put("totalItems", reportsPage.getTotalElements());
            response.put("totalPages", reportsPage.getTotalPages());
            response.put("pageSize", reportsPage.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting pending reports: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取所有举报列表
     */
    @GetMapping
    @Operation(summary = "获取所有举报", description = "获取所有举报记录（含已处理）")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> getAllReports(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ReportResponse> reportsPage = reportService.getAllReports(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reportsPage.getContent());
            response.put("currentPage", reportsPage.getNumber());
            response.put("totalItems", reportsPage.getTotalElements());
            response.put("totalPages", reportsPage.getTotalPages());
            response.put("pageSize", reportsPage.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting all reports: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 处理举报
     */
    @PostMapping("/{id}/handle")
    @Operation(summary = "处理举报", description = "管理员处理举报")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> handleReport(
            @PathVariable Long id,
            @Valid @RequestBody HandleReportRequest request,
            @AuthenticationPrincipal User admin) {

        try {
            ReportResponse report = reportService.handleReport(id, admin.getUsername(), request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "举报处理成功");
            response.put("data", report);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error handling report: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取举报统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取举报统计", description = "获取举报数据统计")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> getReportStatistics() {

        try {
            ReviewReportService.ReportStatistics stats = reportService.getStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting report statistics: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取某评价的所有举报
     */
    @GetMapping("/review/{reviewId}")
    @Operation(summary = "获取评价的举报记录", description = "获取某个评价的所有举报记录")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> getReportsByReview(@PathVariable Long reviewId) {

        try {
            List<ReportResponse> reports = reportService.getReportsByReview(reviewId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", reports);
            response.put("total", reports.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting reports by review: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
