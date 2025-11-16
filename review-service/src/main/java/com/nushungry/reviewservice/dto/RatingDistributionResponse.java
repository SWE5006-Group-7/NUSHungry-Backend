package com.nushungry.reviewservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDistributionResponse {

    private Long stallId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> distribution;
}
