package com.nushungry.service;

import com.nushungry.dto.CreateReportRequest;
import com.nushungry.dto.HandleReportRequest;
import com.nushungry.dto.ReportResponse;
import com.nushungry.model.*;
import com.nushungry.repository.ReviewReportRepository;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 评价举报服务
 */
@Service
public class ReviewReportService {

    @Autowired
    private ReviewReportRepository reportRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建举报
     */
    @Transactional
    public ReportResponse createReport(Long reviewId, Long reporterId, CreateReportRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查是否已经举报过
        if (reportRepository.existsByReviewAndReporter(review, reporter)) {
            throw new RuntimeException("您已经举报过此评价");
        }

        // 创建举报记录
        ReviewReport report = new ReviewReport();
        report.setReview(review);
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setStatus(ReportStatus.PENDING);

        ReviewReport savedReport = reportRepository.save(report);

        return buildReportResponse(savedReport);
    }

    /**
     * 处理举报（管理员）
     */
    @Transactional
    public ReportResponse handleReport(Long reportId, String adminUsername, HandleReportRequest request) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));

        report.setStatus(request.getStatus());
        report.setHandledBy(adminUsername);
        report.setHandledAt(LocalDateTime.now());
        report.setHandleNote(request.getHandleNote());

        ReviewReport savedReport = reportRepository.save(report);

        return buildReportResponse(savedReport);
    }

    /**
     * 获取待处理的举报列表
     */
    public Page<ReportResponse> getPendingReports(Pageable pageable) {
        List<ReportStatus> pendingStatuses = Arrays.asList(ReportStatus.PENDING, ReportStatus.REVIEWING);
        Page<ReviewReport> reports = reportRepository.findByStatusIn(pendingStatuses, pageable);

        return reports.map(this::buildReportResponse);
    }

    /**
     * 获取所有举报列表
     */
    public Page<ReportResponse> getAllReports(Pageable pageable) {
        Page<ReviewReport> reports = reportRepository.findAll(pageable);
        return reports.map(this::buildReportResponse);
    }

    /**
     * 获取某评价的所有举报
     */
    public List<ReportResponse> getReportsByReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        List<ReviewReport> reports = reportRepository.findByReview(review);

        return reports.stream()
                .map(this::buildReportResponse)
                .toList();
    }

    /**
     * 获取用户的举报历史
     */
    public Page<ReportResponse> getUserReports(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Page<ReviewReport> reports = reportRepository.findByReporter(user, pageable);

        return reports.map(this::buildReportResponse);
    }

    /**
     * 获取举报统计
     */
    public ReportStatistics getStatistics() {
        long total = reportRepository.count();
        long pending = reportRepository.countByStatus(ReportStatus.PENDING);
        long reviewing = reportRepository.countByStatus(ReportStatus.REVIEWING);
        long resolved = reportRepository.countByStatus(ReportStatus.RESOLVED);
        long rejected = reportRepository.countByStatus(ReportStatus.REJECTED);

        ReportStatistics stats = new ReportStatistics();
        stats.setTotalCount(total);
        stats.setPendingCount(pending);
        stats.setProcessedCount(resolved);  // resolved映射到processedCount
        stats.setRejectedCount(rejected);

        return stats;
    }

    /**
     * 构建举报响应DTO
     */
    private ReportResponse buildReportResponse(ReviewReport report) {
        Review review = report.getReview();
        User reviewer = review.getUser();
        User reporter = report.getReporter();

        // 构建ReviewInfo
        ReportResponse.ReviewInfo reviewInfo = ReportResponse.ReviewInfo.builder()
                .id(review.getId())
                .comment(review.getComment())
                .rating(review.getRating())
                .user(ReportResponse.UserInfo.builder()
                        .id(reviewer.getId())
                        .username(reviewer.getUsername())
                        .avatarUrl(reviewer.getAvatarUrl())
                        .build())
                .stall(review.getStall() != null ? ReportResponse.StallInfo.builder()
                        .id(review.getStall().getId())
                        .name(review.getStall().getName())
                        .build() : null)
                .build();

        // 构建ReporterInfo
        ReportResponse.UserInfo reporterInfo = ReportResponse.UserInfo.builder()
                .id(reporter.getId())
                .username(reporter.getUsername())
                .avatarUrl(reporter.getAvatarUrl())
                .build();

        return ReportResponse.builder()
                .id(report.getId())
                .review(reviewInfo)
                .reporter(reporterInfo)
                .reason(report.getReason())
                .reasonDisplayName(report.getReason().getDisplayName())
                .description(report.getDescription())
                .status(report.getStatus())
                .statusDisplayName(report.getStatus().getDisplayName())
                .handledBy(report.getHandledBy())
                .handledAt(report.getHandledAt())
                .handleNote(report.getHandleNote())
                .createdAt(report.getCreatedAt())
                .build();
    }

    /**
     * 举报统计数据类
     */
    public static class ReportStatistics {
        private long totalCount;
        private long pendingCount;
        private long processedCount;
        private long rejectedCount;

        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        public long getProcessedCount() { return processedCount; }
        public void setProcessedCount(long processedCount) { this.processedCount = processedCount; }
        public long getRejectedCount() { return rejectedCount; }
        public void setRejectedCount(long rejectedCount) { this.rejectedCount = rejectedCount; }
    }
}
