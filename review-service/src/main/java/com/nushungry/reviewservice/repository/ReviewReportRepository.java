package com.nushungry.reviewservice.repository;

import com.nushungry.reviewservice.document.ReviewReportDocument;
import com.nushungry.reviewservice.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewReportRepository extends MongoRepository<ReviewReportDocument, String> {

    List<ReviewReportDocument> findByReviewId(String reviewId);

    Page<ReviewReportDocument> findByStatus(ReportStatus status, Pageable pageable);

    boolean existsByReviewIdAndReporterId(String reviewId, String reporterId);

    long countByReviewIdAndStatus(String reviewId, ReportStatus status);

    /**
     * 统计各状态的举报数量
     */
    long countByStatus(ReportStatus status);

    /**
     * 统计指定时间范围内的举报数量
     */
    @Query(value = "{ 'createdAt': { $gte: ?0, $lte: ?1 } }", count = true)
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按举报原因统计
     */
    @Query(value = "{ 'reason': ?0 }", count = true)
    long countByReason(String reason);

    /**
     * 统计今日举报数量
     */
    @Query(value = "{ 'createdAt': { $gte: ?0 } }", count = true)
    long countTodayReports(LocalDateTime todayStart);

    /**
     * 统计本周举报数量
     */
    @Query(value = "{ 'createdAt': { $gte: ?0 } }", count = true)
    long countWeekReports(LocalDateTime weekStart);

    /**
     * 统计本月举报数量
     */
    @Query(value = "{ 'createdAt': { $gte: ?0 } }", count = true)
    long countMonthReports(LocalDateTime monthStart);
}
