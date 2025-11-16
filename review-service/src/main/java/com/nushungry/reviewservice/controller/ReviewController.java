package com.nushungry.reviewservice.controller;

import com.nushungry.reviewservice.common.ApiResponse;
import com.nushungry.reviewservice.dto.*;
import com.nushungry.reviewservice.service.RatingCalculationService;
import com.nushungry.reviewservice.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review management APIs")
public class ReviewController {

    private final ReviewService reviewService;
    private final RatingCalculationService ratingCalculationService;

    @PostMapping
    @Operation(summary = "Create a new review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Username") String username,
            @RequestHeader(value = "X-User-Avatar", required = false) String userAvatarUrl) {
        
        ReviewResponse response = reviewService.createReview(request, userId, username, userAvatarUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable String id,
            @Valid @RequestBody UpdateReviewRequest request,
            @RequestHeader("X-User-Id") String userId) {
        
        ReviewResponse response = reviewService.updateReview(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        
        reviewService.deleteReview(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        
        ReviewResponse response = reviewService.getReviewById(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stall/{stallId}")
    @Operation(summary = "Get reviews by stall ID")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByStallId(
            @PathVariable Long stallId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> response = reviewService.getReviewsByStallId(stallId, sortBy, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reviews by user ID")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviewsByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReviewResponse> response = reviewService.getReviewsByUserId(userId, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stall/{stallId}/rating-distribution")
    @Operation(summary = "Get rating distribution for a stall")
    public ResponseEntity<ApiResponse<RatingDistributionResponse>> getRatingDistribution(
            @PathVariable Long stallId) {
        
        RatingDistributionResponse response = ratingCalculationService.getRatingDistribution(stallId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
