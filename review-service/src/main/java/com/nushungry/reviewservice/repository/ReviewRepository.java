package com.nushungry.reviewservice.repository;

import com.nushungry.reviewservice.document.ReviewDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<ReviewDocument, String> {

    Page<ReviewDocument> findByStallIdOrderByCreatedAtDesc(Long stallId, Pageable pageable);

    Page<ReviewDocument> findByStallIdOrderByLikesCountDesc(Long stallId, Pageable pageable);

    Page<ReviewDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByStallId(Long stallId);

    boolean existsByUserIdAndStallId(String userId, Long stallId);

    List<ReviewDocument> findByStallId(Long stallId);

    @Query("{ 'stallId': ?0, 'rating': { $gte: ?1, $lte: ?2 } }")
    Page<ReviewDocument> findByStallIdAndRatingBetween(Long stallId, Integer minRating, Integer maxRating, Pageable pageable);

    // 管理员统计方法
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}
