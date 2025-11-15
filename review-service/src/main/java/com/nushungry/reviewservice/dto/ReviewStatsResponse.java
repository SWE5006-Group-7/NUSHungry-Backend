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
public class ReviewStatsResponse {

    // 原有字段（摊位级别统计）
    private Long stallId;
    private Double averageRating;
    private Long totalReviews;
    private Double averagePrice;
    private Long totalPriceReviews;

    // 管理员统计字段（全局统计）
    private Map<Integer, Long> ratingDistribution; // 1-5星分布
    private Long todayCount; // 今日评价数
    private Long thisWeekCount; // 本周评价数
    private Long thisMonthCount; // 本月评价数
}
