package com.nushungry.repository;

import com.nushungry.model.ModerationStatus;
import com.nushungry.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 根据摊位ID查找评价（按创建时间降序）
     */
    List<Review> findByStallIdOrderByCreatedAtDesc(Long stallId);

    /**
     * 根据摊位ID分页查找评价
     */
    Page<Review> findByStallIdOrderByCreatedAtDesc(Long stallId, Pageable pageable);

    /**
     * 根据用户ID查找评价（按创建时间降序）
     */
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据用户ID分页查找评价
     */
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查找用户对特定摊位的评价
     */
    Optional<Review> findByUserIdAndStallId(Long userId, Long stallId);

    /**
     * 检查用户是否已评价过某个摊位
     */
    boolean existsByUserIdAndStallId(Long userId, Long stallId);

    /**
     * 计算摊位的平均评分
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.stall.id = :stallId")
    Double getAverageRatingByStallId(@Param("stallId") Long stallId);

    /**
     * 统计摊位的评价数量
     */
    long countByStallId(Long stallId);

    /**
     * 统计用户的评价数量
     */
    long countByUserId(Long userId);

    /**
     * 统计指定时间之后创建的评价数量
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);

    /**
     * 获取所有评价的平均评分
     */
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double getAverageRating();

    /**
     * 统计指定时间之前创建的评价数量
     */
    long countByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * 统计指定时间范围内创建的评价数量
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计评分低于指定值且未处理的评价数量
     */
    long countByRatingLessThanAndProcessedFalse(Double rating);

    /**
     * 统计评分低于指定值的评价数量
     */
    long countByRatingLessThan(Double rating);

    /**
     * 获取摊位的最新评价（限制数量）
     */
    List<Review> findTop10ByStallIdOrderByCreatedAtDesc(Long stallId);

    /**
     * 获取用户的最新评价（限制数量）
     */
    List<Review> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    // ==================== 审核相关查询 ====================

    /**
     * 根据审核状态分页查询评价
     */
    Page<Review> findByModerationStatus(ModerationStatus moderationStatus, Pageable pageable);

    /**
     * 统计不同审核状态的评价数量
     */
    long countByModerationStatus(ModerationStatus moderationStatus);

    /**
     * 查询指定时间范围内待审核的评价
     */
    Page<Review> findByModerationStatusAndCreatedAtBetween(
            ModerationStatus moderationStatus,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    /**
     * 获取最新的待审核评价
     */
    List<Review> findTop10ByModerationStatusOrderByCreatedAtDesc(ModerationStatus moderationStatus);

    // ==================== 筛选和排序查询 ====================

    /**
     * 按评分范围筛选评价
     */
    Page<Review> findByStallIdAndRatingBetween(Long stallId, Double minRating, Double maxRating, Pageable pageable);

    /**
     * 按摊位ID和审核状态查找评价
     */
    Page<Review> findByStallIdAndModerationStatus(Long stallId, ModerationStatus moderationStatus, Pageable pageable);

    /**
     * 获取热门评价（综合算法：点赞数 + 评分 + 时间新鲜度）
     * 算法说明：
     * - 点赞数权重：30%
     * - 评分权重：40%
     * - 时间新鲜度权重：30%（使用天数衰减）
     * 热度分数 = (点赞数/10) * 0.3 + (评分/5) * 0.4 + (1/(1+天数)) * 0.3
     */
    @Query("SELECT r FROM Review r WHERE r.stall.id = :stallId AND r.moderationStatus = 'APPROVED' " +
           "ORDER BY " +
           "(r.likeCount / 10.0) * 0.3 + " +
           "(r.rating / 5.0) * 0.4 + " +
           "(1.0 / (1.0 + FUNCTION('DATEDIFF', CURRENT_DATE, r.createdAt))) * 0.3 DESC")
    Page<Review> findHotReviewsByStallId(@Param("stallId") Long stallId, Pageable pageable);

    /**
     * 获取全局热门评价（跨所有摊位）
     */
    @Query("SELECT r FROM Review r WHERE r.moderationStatus = 'APPROVED' " +
           "ORDER BY " +
           "(r.likeCount / 10.0) * 0.3 + " +
           "(r.rating / 5.0) * 0.4 + " +
           "(1.0 / (1.0 + FUNCTION('DATEDIFF', CURRENT_DATE, r.createdAt))) * 0.3 DESC")
    Page<Review> findGlobalHotReviews(Pageable pageable);

    /**
     * 按点赞数降序获取评价
     */
    Page<Review> findByStallIdOrderByLikeCountDesc(Long stallId, Pageable pageable);

    /**
     * 按评分降序获取评价
     */
    Page<Review> findByStallIdOrderByRatingDesc(Long stallId, Pageable pageable);

    /**
     * 获取高分评价（4星及以上）
     */
    @Query("SELECT r FROM Review r WHERE r.stall.id = :stallId AND r.rating >= 4.0 AND r.moderationStatus = 'APPROVED' " +
           "ORDER BY r.rating DESC, r.likeCount DESC, r.createdAt DESC")
    Page<Review> findHighRatedReviewsByStallId(@Param("stallId") Long stallId, Pageable pageable);

    /**
     * 获取低分评价（2星及以下）
     */
    @Query("SELECT r FROM Review r WHERE r.stall.id = :stallId AND r.rating <= 2.0 AND r.moderationStatus = 'APPROVED' " +
           "ORDER BY r.rating ASC, r.createdAt DESC")
    Page<Review> findLowRatedReviewsByStallId(@Param("stallId") Long stallId, Pageable pageable);

    /**
     * 获取有图评价
     */
    @Query("SELECT r FROM Review r WHERE r.stall.id = :stallId AND SIZE(r.imageUrls) > 0 AND r.moderationStatus = 'APPROVED' " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findReviewsWithImagesByStallId(@Param("stallId") Long stallId, Pageable pageable);
}