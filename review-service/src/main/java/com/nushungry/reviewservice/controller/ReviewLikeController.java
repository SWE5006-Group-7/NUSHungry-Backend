package com.nushungry.reviewservice.controller;

import com.nushungry.reviewservice.common.ApiResponse;
import com.nushungry.reviewservice.service.ReviewLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Like", description = "Review like management APIs")
public class ReviewLikeController {

    private final ReviewLikeService reviewLikeService;

    @PostMapping("/{id}/like")
    @Operation(summary = "Toggle like on a review")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean isLiked = reviewLikeService.toggleLike(id, userId);
        long likeCount = reviewLikeService.getLikeCount(id);
        
        Map<String, Object> response = Map.of(
                "isLiked", isLiked,
                "likeCount", likeCount
        );
        
        String message = isLiked ? "Review liked successfully" : "Review unliked successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/{id}/is-liked")
    @Operation(summary = "Check if user has liked a review")
    public ResponseEntity<ApiResponse<Boolean>> isLikedByUser(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        
        boolean isLiked = reviewLikeService.isLikedByUser(id, userId);
        return ResponseEntity.ok(ApiResponse.success(isLiked));
    }

    @GetMapping("/{id}/like-count")
    @Operation(summary = "Get like count for a review")
    public ResponseEntity<ApiResponse<Long>> getLikeCount(@PathVariable String id) {
        long likeCount = reviewLikeService.getLikeCount(id);
        return ResponseEntity.ok(ApiResponse.success(likeCount));
    }
}
