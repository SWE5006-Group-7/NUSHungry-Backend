package com.nushungry.reviewservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private String id;
    private Long stallId;
    private String stallName;
    private String userId;
    private String username;
    private String userAvatarUrl;
    private Integer rating;
    private String comment;
    private List<String> imageUrls;
    private Double totalCost;
    private Integer numberOfPeople;
    private Integer likesCount;
    private Boolean isLikedByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
