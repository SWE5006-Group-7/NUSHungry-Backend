package com.nushungry.repository;

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

    // ==================== 管理员查询 ====================

    /**
     * 根据评论内容关键词查找评价
     */
    Page<Review> findByCommentContaining(String keyword, Pageable pageable);

    /**
     * 根据评分查找评价
     */
    Page<Review> findByRating(Double rating, Pageable pageable);

    /**
     * 根据评论内容和评分查找评价
     */
    Page<Review> findByCommentContainingAndRating(String keyword, Double rating, Pageable pageable);

    /**
     * 获取全局平均评分
     */
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double findAverageRating();

    /**
     * 统计摊位各评分的数量（评分分布）
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.stall.id = :stallId GROUP BY r.rating")
    List<Object[]> getRatingDistributionByStallId(@Param("stallId") Long stallId);

    /**
     * 根据摊位ID和排序方式查找评价
     */
    @Query("SELECT r FROM Review r WHERE r.stall.id = :stallId ORDER BY " +
           "CASE WHEN :sortBy = 'likesCount' THEN r.likesCount END DESC, " +
           "CASE WHEN :sortBy = 'createdAt' THEN r.createdAt END DESC")
    Page<Review> findByStallIdWithSort(@Param("stallId") Long stallId,
                                        @Param("sortBy") String sortBy,
                                        Pageable pageable);
}
