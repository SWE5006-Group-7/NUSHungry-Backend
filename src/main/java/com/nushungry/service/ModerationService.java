package com.nushungry.service;

import com.nushungry.dto.BatchModerationRequest;
import com.nushungry.dto.ModerationRequest;
import com.nushungry.model.ModerationLog;
import com.nushungry.model.ModerationStatus;
import com.nushungry.model.Review;
import com.nushungry.model.User;
import com.nushungry.repository.ModerationLogRepository;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ReviewRepository reviewRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final UserRepository userRepository;

    /**
     * 获取待审核的评价列表
     */
    public Page<Review> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(ModerationStatus.PENDING, pageable);
    }

    /**
     * 获取所有审核状态的评价统计
     */
    public Map<String, Long> getModerationStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", reviewRepository.countByModerationStatus(ModerationStatus.PENDING));
        stats.put("approved", reviewRepository.countByModerationStatus(ModerationStatus.APPROVED));
        stats.put("rejected", reviewRepository.countByModerationStatus(ModerationStatus.REJECTED));
        stats.put("total", reviewRepository.count());
        return stats;
    }

    /**
     * 审核单个评价
     */
    @Transactional
    public Review moderateReview(Long reviewId, ModerationRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        // 验证审核动作
        if (request.getAction() != ModerationStatus.APPROVED &&
            request.getAction() != ModerationStatus.REJECTED) {
            throw new RuntimeException("无效的审核动作");
        }

        // 如果是驳回,原因不能为空
        if (request.getAction() == ModerationStatus.REJECTED &&
            (request.getReason() == null || request.getReason().trim().isEmpty())) {
            throw new RuntimeException("驳回评价时必须填写原因");
        }

        // 获取当前登录的管理员
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User moderator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("审核人不存在"));

        // 更新评价状态
        review.setModerationStatus(request.getAction());
        review.setModeratedAt(LocalDateTime.now());
        review.setModeratedBy(username);
        if (request.getAction() == ModerationStatus.REJECTED) {
            review.setRejectReason(request.getReason());
        }
        Review savedReview = reviewRepository.save(review);

        // 记录审核日志
        ModerationLog log = new ModerationLog();
        log.setReview(review);
        log.setModerator(moderator);
        log.setAction(request.getAction());
        log.setReason(request.getReason());
        moderationLogRepository.save(log);

        return savedReview;
    }

    /**
     * 批量审核评价
     */
    @Transactional
    public Map<String, Object> batchModerateReviews(BatchModerationRequest request) {
        // 验证审核动作
        if (request.getAction() != ModerationStatus.APPROVED &&
            request.getAction() != ModerationStatus.REJECTED) {
            throw new RuntimeException("无效的审核动作");
        }

        // 获取当前登录的管理员
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User moderator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("审核人不存在"));

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (Long reviewId : request.getReviewIds()) {
            try {
                Review review = reviewRepository.findById(reviewId).orElse(null);
                if (review == null) {
                    failedIds.add(reviewId);
                    continue;
                }

                // 更新评价状态
                review.setModerationStatus(request.getAction());
                review.setModeratedAt(LocalDateTime.now());
                review.setModeratedBy(username);
                if (request.getAction() == ModerationStatus.REJECTED) {
                    review.setRejectReason(request.getReason());
                }
                reviewRepository.save(review);

                // 记录审核日志
                ModerationLog log = new ModerationLog();
                log.setReview(review);
                log.setModerator(moderator);
                log.setAction(request.getAction());
                log.setReason(request.getReason());
                moderationLogRepository.save(log);

                successIds.add(reviewId);
            } catch (Exception e) {
                failedIds.add(reviewId);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", successIds.size());
        result.put("failed", failedIds.size());
        result.put("successIds", successIds);
        result.put("failedIds", failedIds);
        return result;
    }

    /**
     * 获取审核日志
     */
    public Page<ModerationLog> getModerationLogs(Pageable pageable) {
        return moderationLogRepository.findAll(pageable);
    }

    /**
     * 根据评价ID获取审核日志
     */
    public List<ModerationLog> getModerationLogsByReviewId(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        return moderationLogRepository.findByReview(review);
    }
}
