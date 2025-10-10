package com.nushungry.repository;

import com.nushungry.model.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 评价举报Repository
 */
@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    /**
     * 检查用户是否已举报某评价
     */
    boolean existsByReviewIdAndReporterId(Long reviewId, Long reporterId);

    /**
     * 按状态分页查询举报记录
     */
    Page<ReviewReport> findByStatus(ReviewReport.ReportStatus status, Pageable pageable);

    /**
     * 查询某评价的所有举报记录
     */
    Page<ReviewReport> findByReviewId(Long reviewId, Pageable pageable);

    /**
     * 统计待处理的举报数量
     */
    long countByStatus(ReviewReport.ReportStatus status);

    /**
     * 统计某评价被举报的次数
     */
    long countByReviewId(Long reviewId);

    /**
     * 获取举报统计
     */
    @Query("SELECT rr.status, COUNT(rr) FROM ReviewReport rr GROUP BY rr.status")
    java.util.List<Object[]> getReportStats();
}
