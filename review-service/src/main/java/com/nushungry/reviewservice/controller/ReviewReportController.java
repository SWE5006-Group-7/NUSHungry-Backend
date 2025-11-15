package com.nushungry.reviewservice.controller;

import com.nushungry.reviewservice.common.ApiResponse;
import com.nushungry.reviewservice.dto.CreateReportRequest;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.dto.ReportResponse;
import com.nushungry.reviewservice.enums.ReportStatus;
import com.nushungry.reviewservice.service.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Report", description = "Review report management APIs")
public class ReviewReportController {

    private final ReviewReportService reviewReportService;

    @PostMapping("/{id}/report")
    @Operation(summary = "Report a review")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @PathVariable String id,
            @Valid @RequestBody CreateReportRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username) {
        
        ReportResponse response = reviewReportService.createReport(id, request, userId, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Report submitted successfully", response));
    }

    @GetMapping("/{id}/reports")
    @Operation(summary = "Get reports for a review (Admin only)")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByReviewId(
            @PathVariable String id,
            @RequestHeader("X-User-Role") String userRole) {
        
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }
        
        List<ReportResponse> response = reviewReportService.getReportsByReviewId(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reports/status/{status}")
    @Operation(summary = "Get reports by status (Admin only)")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReportsByStatus(
            @PathVariable ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-User-Role") String userRole) {
        
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReportResponse> response = reviewReportService.getReportsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/reports/{id}/handle")
    @Operation(summary = "Handle a report (Admin only)")
    public ResponseEntity<ApiResponse<ReportResponse>> handleReport(
            @PathVariable String id,
            @Valid @RequestBody HandleReportRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        if (!"ROLE_ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }
        
        ReportResponse response = reviewReportService.handleReport(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Report handled successfully", response));
    }
}
