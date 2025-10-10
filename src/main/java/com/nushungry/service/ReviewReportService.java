package com.nushungry.service;

import com.nushungry.dto.CreateReportRequest;
import com.nushungry.dto.HandleReportRequest;
import com.nushungry.model.Review;
import com.nushungry.model.ReviewReport;
import com.nushungry.model.User;
import com.nushungry.repository.ReviewReportRepository;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价举报服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /**
     * 创建举报
     */
    @Transactional
    public ReviewReport createReport(Long reviewId, Long reporterId, CreateReportRequest request) {
        // 检查是否已举报过
        if (reviewReportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)) {
            throw new RuntimeException("您已经举报过此评价");
        }

        // 检查评价是否存在
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        // 检查用户是否存在
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 创建举报记录
        ReviewReport report = new ReviewReport();
        report.setReview(review);
        report.setReporter(reporter);
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setStatus(ReviewReport.ReportStatus.PENDING);

        ReviewReport savedReport = reviewReportRepository.save(report);
        log.info("用户 {} 举报了评价 {}, 原因: {}", reporterId, reviewId, request.getReason());

        return savedReport;
    }

    /**
     * 处理举报（管理员使用）
     */
    @Transactional
    public ReviewReport handleReport(Long reportId, Long handlerId, HandleReportRequest request) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));

        if (report.getStatus() != ReviewReport.ReportStatus.PENDING) {
            throw new RuntimeException("该举报已经被处理");
        }

        // 更新举报状态
        report.setStatus(request.getStatus());
        report.setHandlerId(handlerId);
        report.setHandlerNote(request.getHandlerNote());
        report.setHandledAt(LocalDateTime.now());

        // 如果接受举报且需要删除评价
        if (request.getStatus() == ReviewReport.ReportStatus.ACCEPTED &&
            Boolean.TRUE.equals(request.getDeleteReview())) {
            Review review = report.getReview();
            reviewRepository.delete(review);
            log.info("管理员 {} 删除了被举报的评价 {}", handlerId, review.getId());
        }

        ReviewReport savedReport = reviewReportRepository.save(report);
        log.info("管理员 {} 处理了举报 {}, 状态: {}", handlerId, reportId, request.getStatus());

        return savedReport;
    }

    /**
     * 获取待处理的举报列表
     */
    public Page<ReviewReport> getPendingReports(Pageable pageable) {
        return reviewReportRepository.findByStatus(ReviewReport.ReportStatus.PENDING, pageable);
    }

    /**
     * 按状态获取举报列表
     */
    public Page<ReviewReport> getReportsByStatus(ReviewReport.ReportStatus status, Pageable pageable) {
        return reviewReportRepository.findByStatus(status, pageable);
    }

    /**
     * 获取某评价的所有举报记录
     */
    public Page<ReviewReport> getReportsByReviewId(Long reviewId, Pageable pageable) {
        return reviewReportRepository.findByReviewId(reviewId, pageable);
    }

    /**
     * 获取举报统计
     */
    public Map<String, Long> getReportStats() {
        Map<String, Long> stats = new HashMap<>();
        List<Object[]> results = reviewReportRepository.getReportStats();

        // 初始化所有状态为0
        for (ReviewReport.ReportStatus status : ReviewReport.ReportStatus.values()) {
            stats.put(status.name(), 0L);
        }

        // 填充实际数据
        for (Object[] result : results) {
            ReviewReport.ReportStatus status = (ReviewReport.ReportStatus) result[0];
            Long count = (Long) result[1];
            stats.put(status.name(), count);
        }

        // 计算总数
        stats.put("TOTAL", stats.values().stream().mapToLong(Long::longValue).sum());

        return stats;
    }

    /**
     * 检查用户是否已举报某评价
     */
    public boolean hasUserReported(Long reviewId, Long reporterId) {
        return reviewReportRepository.existsByReviewIdAndReporterId(reviewId, reporterId);
    }
}
