package com.nushungry.repository;

import com.nushungry.model.Review;
import com.nushungry.model.ReviewLike;
import com.nushungry.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    /**
     * 检查用户是否已点赞某评价
     */
    boolean existsByReviewAndUser(Review review, User user);

    /**
     * 查找用户对某评价的点赞记录
     */
    Optional<ReviewLike> findByReviewAndUser(Review review, User user);

    /**
     * 统计评价的点赞数
     */
    long countByReview(Review review);

    /**
     * 删除用户对某评价的点赞
     */
    void deleteByReviewAndUser(Review review, User user);

    /**
     * 批量检查用户对多个评价的点赞状态
     */
    @Query("SELECT rl.review.id FROM ReviewLike rl WHERE rl.user.id = :userId AND rl.review.id IN :reviewIds")
    List<Long> findLikedReviewIdsByUserAndReviewIds(@Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);
}
