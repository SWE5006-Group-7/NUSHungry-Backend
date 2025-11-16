package com.nushungry.repository;

import com.nushungry.model.Review;
import com.nushungry.model.ReviewReport;
import com.nushungry.model.ReportStatus;
import com.nushungry.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    /**
     * 检查用户是否已举报某评价
     */
    boolean existsByReviewAndReporter(Review review, User reporter);

    /**
     * 查询某评价的所有举报
     */
    List<ReviewReport> findByReview(Review review);

    /**
     * 按状态查询举报列表
     */
    Page<ReviewReport> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * 查询待处理的举报
     */
    Page<ReviewReport> findByStatusIn(List<ReportStatus> statuses, Pageable pageable);

    /**
     * 统计某评价的举报次数
     */
    long countByReview(Review review);

    /**
     * 统计待处理举报数量
     */
    long countByStatus(ReportStatus status);

    /**
     * 查询某时间段内的举报
     */
    @Query("SELECT r FROM ReviewReport r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<ReviewReport> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 查询用户的举报历史
     */
    Page<ReviewReport> findByReporter(User reporter, Pageable pageable);
}
