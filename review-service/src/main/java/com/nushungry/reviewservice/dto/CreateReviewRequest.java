package com.nushungry.reviewservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "Stall ID is required")
    private Long stallId;

    // stallName 可选,用于客户端提供档口名称以避免额外查询
    // 如果未提供,服务端可以从 stallId 查询获取
    private String stallName;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @NotBlank(message = "Comment is required")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    private List<String> imageUrls;

    @Min(value = 0, message = "Total cost must be non-negative")
    private Double totalCost;

    @Min(value = 1, message = "Number of people must be at least 1")
    private Integer numberOfPeople;
}
