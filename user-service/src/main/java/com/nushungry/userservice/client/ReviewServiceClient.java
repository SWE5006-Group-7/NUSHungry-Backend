package com.nushungry.userservice.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feign客户端 - 调用Review Service
 */
@FeignClient(
    name = "review-service",
    url = "${services.review-service.url}",
    fallback = ReviewServiceClientFallback.class
)
public interface ReviewServiceClient {

    /**
     * 获取评价统计信息
     */
    @GetMapping("/admin/dashboard/stats")
    ReviewStatsResponse getReviewStats();

    /**
     * 获取最新评价列表
     */
    @GetMapping("/admin/dashboard/latest-reviews")
    List<LatestReviewResponse> getLatestReviews();

    /**
     * 评价统计响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "评价统计响应")
    class ReviewStatsResponse {
        @Schema(description = "总评价数")
        private Integer totalReviews;

        @Schema(description = "昨天的评价数")
        private Integer yesterdayReviews;

        @Schema(description = "今日评价数")
        private Integer todayReviews;

        @Schema(description = "昨日评价数（对比）")
        private Integer yesterdayReviewsForToday;

        @Schema(description = "待处理投诉数")
        private Integer pendingComplaints;

        @Schema(description = "总投诉数")
        private Integer totalComplaints;
    }

    /**
     * 最新评价响应
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "最新评价响应")
    class LatestReviewResponse {
        @Schema(description = "评价ID")
        private String id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "档口名称")
        private String stallName;

        @Schema(description = "评分")
        private Double rating;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}
