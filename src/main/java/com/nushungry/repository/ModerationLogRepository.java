package com.nushungry.repository;

import com.nushungry.model.ModerationLog;
import com.nushungry.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {

    /**
     * 根据评价ID查询审核日志
     */
    List<ModerationLog> findByReview(Review review);

    /**
     * 根据评价ID查询审核日志(分页)
     */
    Page<ModerationLog> findByReview(Review review, Pageable pageable);

    /**
     * 根据审核人ID查询审核日志
     */
    Page<ModerationLog> findByModeratorId(Long moderatorId, Pageable pageable);
}
