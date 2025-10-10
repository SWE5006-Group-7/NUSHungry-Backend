package com.nushungry.repository;

import com.nushungry.model.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 评价点赞Repository
 */
@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    /**
     * 检查用户是否已点赞某评价
     */
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    /**
     * 查找用户对某评价的点赞记录
     */
    Optional<ReviewLike> findByReviewIdAndUserId(Long reviewId, Long userId);

    /**
     * 统计某评价的点赞数
     */
    long countByReviewId(Long reviewId);

    /**
     * 删除用户对某评价的点赞
     */
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    /**
     * 获取用户点赞过的所有评价ID列表
     */
    @Query("SELECT rl.review.id FROM ReviewLike rl WHERE rl.user.id = :userId")
    java.util.List<Long> findReviewIdsByUserId(@Param("userId") Long userId);
}
