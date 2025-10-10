package com.nushungry.controller;

import com.nushungry.service.ReviewLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价点赞控制器
 */
@RestController
@RequestMapping("/api/reviews/{reviewId}/likes")
@RequiredArgsConstructor
@Slf4j
public class ReviewLikeController {

    private final ReviewLikeService reviewLikeService;

    /**
     * 点赞评价
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> likeReview(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        reviewLikeService.likeReview(reviewId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "点赞成功");
        response.put("likeCount", reviewLikeService.getLikeCount(reviewId));

        return ResponseEntity.ok(response);
    }

    /**
     * 取消点赞
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> unlikeReview(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        reviewLikeService.unlikeReview(reviewId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "取消点赞成功");
        response.put("likeCount", reviewLikeService.getLikeCount(reviewId));

        return ResponseEntity.ok(response);
    }

    /**
     * 检查当前用户是否已点赞
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkLikeStatus(@PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        boolean liked = reviewLikeService.hasUserLiked(reviewId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", liked);
        response.put("likeCount", reviewLikeService.getLikeCount(reviewId));

        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户点赞的所有评价ID列表
     */
    @GetMapping("/user/liked-reviews")
    public ResponseEntity<List<Long>> getUserLikedReviews() {
        Long userId = getCurrentUserId();
        List<Long> likedReviewIds = reviewLikeService.getUserLikedReviewIds(userId);
        return ResponseEntity.ok(likedReviewIds);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        String username = authentication.getName();
        // 假设username就是userId（根据实际情况调整）
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            throw new RuntimeException("无法获取用户ID");
        }
    }
}
