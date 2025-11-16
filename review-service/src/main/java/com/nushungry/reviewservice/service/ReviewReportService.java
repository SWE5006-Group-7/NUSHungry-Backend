package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewReportDocument;
import com.nushungry.reviewservice.dto.CreateReportRequest;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.dto.ReportResponse;
import com.nushungry.reviewservice.dto.ReportStatistics;
import com.nushungry.reviewservice.enums.ReportReason;
import com.nushungry.reviewservice.enums.ReportStatus;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.ValidationException;
import com.nushungry.reviewservice.repository.ReviewReportRepository;
import com.nushungry.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;
    private final MongoTemplate mongoTemplate;

    @Transactional
    public ReportResponse createReport(String reviewId, CreateReportRequest request, String reporterId, String reporterName) {
        log.info("Creating report for review ID: {} by user: {}", reviewId, reporterId);

        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review", "id", reviewId);
        }

        if (reviewReportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)) {
            throw new ValidationException("You have already reported this review");
        }

        ReviewReportDocument report = ReviewReportDocument.builder()
                .reviewId(reviewId)
                .reporterId(reporterId)
                .reporterName(reporterName)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        ReviewReportDocument savedReport = reviewReportRepository.save(report);
        log.info("Report created with ID: {}", savedReport.getId());

        return mapToResponse(savedReport);
    }

    public List<ReportResponse> getReportsByReviewId(String reviewId) {
        log.info("Getting reports for review ID: {}", reviewId);
        List<ReviewReportDocument> reports = reviewReportRepository.findByReviewId(reviewId);
        return reports.stream().map(this::mapToResponse).toList();
    }

    public Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        log.info("Getting reports by status: {}", status);
        Page<ReviewReportDocument> reports = reviewReportRepository.findByStatus(status, pageable);
        return reports.map(this::mapToResponse);
    }

    @Transactional
    public ReportResponse handleReport(String reportId, HandleReportRequest request, String handlerUserId) {
        log.info("Handling report ID: {} by user: {}", reportId, handlerUserId);

        ReviewReportDocument report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        report.setStatus(request.getStatus());
        report.setHandledBy(handlerUserId);
        report.setHandledAt(LocalDateTime.now());
        report.setHandleNote(request.getHandleNote());

        ReviewReportDocument updatedReport = reviewReportRepository.save(report);
        log.info("Report handled successfully");

        return mapToResponse(updatedReport);
    }

    /**
     * 获取待处理的举报列表（管理员功能）
     */
    public Page<ReportResponse> getPendingReports(Pageable pageable) {
        log.info("Getting pending reports for admin");

        Query query = new Query();
        query.addCriteria(Criteria.where("status").in(Arrays.asList(ReportStatus.PENDING, ReportStatus.REVIEWING)));

        // 添加排序
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        query.with(sort);

        // 执行查询
        long total = mongoTemplate.count(query, ReviewReportDocument.class);
        query.skip((long) pageable.getPageNumber() * pageable.getPageSize());
        query.limit(pageable.getPageSize());

        List<ReviewReportDocument> reports = mongoTemplate.find(query, ReviewReportDocument.class);

        return new org.springframework.data.domain.PageImpl<>(
            reports.stream().map(this::mapToResponse).collect(Collectors.toList()),
            pageable,
            total
        );
    }

    /**
     * 获取所有举报列表（管理员功能）
     */
    public Page<ReportResponse> getAllReports(Pageable pageable) {
        log.info("Getting all reports for admin");

        Page<ReviewReportDocument> reports = reviewReportRepository.findAll(pageable);
        return reports.map(this::mapToResponse);
    }

    /**
     * 获取举报统计数据（管理员功能）
     */
    public ReportStatistics getStatistics() {
        log.info("Getting report statistics for admin");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);

        // 基础统计
        long totalCount = reviewReportRepository.count();
        long pendingCount = reviewReportRepository.countByStatus(ReportStatus.PENDING);
        long reviewingCount = reviewReportRepository.countByStatus(ReportStatus.REVIEWING);
        long processedCount = reviewReportRepository.countByStatus(ReportStatus.RESOLVED);
        long rejectedCount = reviewReportRepository.countByStatus(ReportStatus.REJECTED);

        // 时间统计
        long todayCount = reviewReportRepository.countTodayReports(todayStart);
        long thisWeekCount = reviewReportRepository.countWeekReports(weekStart);
        long thisMonthCount = reviewReportRepository.countMonthReports(monthStart);

        // 原因分布统计
        Map<String, Long> reasonDistribution = new HashMap<>();
        for (ReportReason reason : ReportReason.values()) {
            long count = reviewReportRepository.countByReason(reason.name());
            if (count > 0) {
                reasonDistribution.put(reason.name(), count);
            }
        }

        // 状态分布统计
        Map<String, Long> statusDistribution = new HashMap<>();
        statusDistribution.put("PENDING", pendingCount);
        statusDistribution.put("REVIEWING", reviewingCount);
        statusDistribution.put("RESOLVED", processedCount);
        statusDistribution.put("REJECTED", rejectedCount);

        // 计算平均处理时间
        Double averageHandlingTimeHours = calculateAverageHandlingTime();

        // 计算处理率
        Double processingRate = totalCount > 0 ? (double) processedCount / totalCount : 0.0;

        return ReportStatistics.builder()
                .totalCount(totalCount)
                .pendingCount(pendingCount)
                .reviewingCount(reviewingCount)
                .processedCount(processedCount)
                .rejectedCount(rejectedCount)
                .todayCount(todayCount)
                .thisWeekCount(thisWeekCount)
                .thisMonthCount(thisMonthCount)
                .reasonDistribution(reasonDistribution)
                .statusDistribution(statusDistribution)
                .averageHandlingTimeHours(averageHandlingTimeHours)
                .processingRate(processingRate)
                .build();
    }

    /**
     * 计算平均处理时间（小时）
     */
    private Double calculateAverageHandlingTime() {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").in(Arrays.asList(ReportStatus.RESOLVED, ReportStatus.REJECTED)));
        query.addCriteria(Criteria.where("handledAt").ne(null));

        List<ReviewReportDocument> handledReports = mongoTemplate.find(query, ReviewReportDocument.class);

        if (handledReports.isEmpty()) {
            return null;
        }

        double totalHours = 0;
        int count = 0;

        for (ReviewReportDocument report : handledReports) {
            if (report.getHandledAt() != null && report.getCreatedAt() != null) {
                long hours = ChronoUnit.HOURS.between(report.getCreatedAt(), report.getHandledAt());
                totalHours += hours;
                count++;
            }
        }

        return count > 0 ? totalHours / count : null;
    }

    private ReportResponse mapToResponse(ReviewReportDocument document) {
        return ReportResponse.builder()
                .id(document.getId())
                .reviewId(document.getReviewId())
                .reporterId(document.getReporterId())
                .reporterName(document.getReporterName())
                .reason(document.getReason())
                .description(document.getDescription())
                .status(document.getStatus())
                .handledBy(document.getHandledBy())
                .handledAt(document.getHandledAt())
                .handleNote(document.getHandleNote())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
