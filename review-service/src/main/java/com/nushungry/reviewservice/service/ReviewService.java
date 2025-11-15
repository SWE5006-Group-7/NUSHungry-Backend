package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.CreateReviewRequest;
import com.nushungry.reviewservice.dto.ReviewResponse;
import com.nushungry.reviewservice.dto.UpdateReviewRequest;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.UnauthorizedException;
import com.nushungry.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeService reviewLikeService;
    private final RatingCalculationService ratingCalculationService;
    private final PriceCalculationService priceCalculationService;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, String userId, String username, String userAvatarUrl) {
        log.info("Creating review for stall ID: {} by user: {}", request.getStallId(), userId);

        ReviewDocument review = ReviewDocument.builder()
                .stallId(request.getStallId())
                .stallName(request.getStallName())
                .userId(userId)
                .username(username)
                .userAvatarUrl(userAvatarUrl)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrls(request.getImageUrls())
                .totalCost(request.getTotalCost())
                .numberOfPeople(request.getNumberOfPeople())
                .likesCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReviewDocument savedReview = reviewRepository.save(review);
        log.info("Review created with ID: {}", savedReview.getId());

        ratingCalculationService.calculateAndPublishRating(request.getStallId());
        priceCalculationService.calculateAndPublishPrice(request.getStallId());

        return mapToResponse(savedReview, userId);
    }

    @Transactional
    public ReviewResponse updateReview(String reviewId, UpdateReviewRequest request, String userId) {
        log.info("Updating review ID: {} by user: {}", reviewId, userId);

        ReviewDocument review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        checkOwnership(review, userId);

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }
        if (request.getImageUrls() != null) {
            review.setImageUrls(request.getImageUrls());
        }
        if (request.getTotalCost() != null) {
            review.setTotalCost(request.getTotalCost());
        }
        if (request.getNumberOfPeople() != null) {
            review.setNumberOfPeople(request.getNumberOfPeople());
        }
        review.setUpdatedAt(LocalDateTime.now());

        ReviewDocument updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully");

        ratingCalculationService.calculateAndPublishRating(review.getStallId());
        priceCalculationService.calculateAndPublishPrice(review.getStallId());

        return mapToResponse(updatedReview, userId);
    }

    @Transactional
    public void deleteReview(String reviewId, String userId) {
        log.info("Deleting review ID: {} by user: {}", reviewId, userId);

        ReviewDocument review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        checkOwnership(review, userId);

        Long stallId = review.getStallId();
        reviewRepository.delete(review);
        log.info("Review deleted successfully");

        ratingCalculationService.calculateAndPublishRating(stallId);
        priceCalculationService.calculateAndPublishPrice(stallId);
    }

    public ReviewResponse getReviewById(String reviewId, String currentUserId) {
        log.info("Getting review ID: {}", reviewId);
        ReviewDocument review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        return mapToResponse(review, currentUserId);
    }

    public Page<ReviewResponse> getReviewsByStallId(Long stallId, String sortBy, String currentUserId, Pageable pageable) {
        log.info("Getting reviews for stall ID: {} sorted by: {}", stallId, sortBy);
        
        Page<ReviewDocument> reviews;
        if ("likes".equalsIgnoreCase(sortBy)) {
            reviews = reviewRepository.findByStallIdOrderByLikesCountDesc(stallId, pageable);
        } else {
            reviews = reviewRepository.findByStallIdOrderByCreatedAtDesc(stallId, pageable);
        }

        return reviews.map(review -> mapToResponse(review, currentUserId));
    }

    public Page<ReviewResponse> getReviewsByUserId(String userId, String currentUserId, Pageable pageable) {
        log.info("Getting reviews by user ID: {}", userId);
        Page<ReviewDocument> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return reviews.map(review -> mapToResponse(review, currentUserId));
    }

    private void checkOwnership(ReviewDocument review, String userId) {
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to modify this review");
        }
    }

    private ReviewResponse mapToResponse(ReviewDocument document, String currentUserId) {
        boolean isLiked = currentUserId != null && reviewLikeService.isLikedByUser(document.getId(), currentUserId);
        
        return ReviewResponse.builder()
                .id(document.getId())
                .stallId(document.getStallId())
                .stallName(document.getStallName())
                .userId(document.getUserId())
                .username(document.getUsername())
                .userAvatarUrl(document.getUserAvatarUrl())
                .rating(document.getRating())
                .comment(document.getComment())
                .imageUrls(document.getImageUrls())
                .totalCost(document.getTotalCost())
                .numberOfPeople(document.getNumberOfPeople())
                .likesCount(document.getLikesCount())
                .isLikedByCurrentUser(isLiked)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
