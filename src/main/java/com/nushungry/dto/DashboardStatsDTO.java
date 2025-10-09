package com.nushungry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表板统计数据")
public class DashboardStatsDTO {

    @Schema(description = "统计卡片数据")
    private StatsCards statsCards;

    @Schema(description = "系统概览")
    private SystemOverview systemOverview;

    @Schema(description = "用户增长数据")
    private List<UserGrowthData> userGrowthData;

    @Schema(description = "最新用户列表")
    private List<LatestUser> latestUsers;

    @Schema(description = "最新评价列表")
    private List<LatestReview> latestReviews;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "统计卡片")
    public static class StatsCards {
        @Schema(description = "总用户数")
        private Integer totalUsers;

        @Schema(description = "用户增长趋势百分比")
        private Double userTrend;

        @Schema(description = "总摊位数")
        private Integer totalStalls;

        @Schema(description = "摊位增长趋势百分比")
        private Double stallTrend;

        @Schema(description = "总评价数")
        private Integer totalReviews;

        @Schema(description = "评价增长趋势百分比")
        private Double reviewTrend;

        @Schema(description = "今日订单数")
        private Integer todayOrders;

        @Schema(description = "订单增长趋势百分比")
        private Double orderTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "系统概览")
    public static class SystemOverview {
        @Schema(description = "系统运行时间（天）")
        private Long runningDays;

        @Schema(description = "活跃用户数")
        private Integer activeUsers;

        @Schema(description = "活跃用户百分比")
        private Double activeUserPercentage;

        @Schema(description = "待处理投诉")
        private Integer pendingComplaints;

        @Schema(description = "待处理投诉百分比")
        private Double pendingComplaintPercentage;

        @Schema(description = "系统健康度评分")
        private Integer healthScore;

        @Schema(description = "系统健康状态")
        private String healthStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户增长数据")
    public static class UserGrowthData {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "用户数量")
        private Integer count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "最新用户")
    public static class LatestUser {
        @Schema(description = "用户ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "是否启用")
        private Boolean enabled;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "最新评价")
    public static class LatestReview {
        @Schema(description = "评价ID")
        private Long id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "摊位名称")
        private String stallName;

        @Schema(description = "评分")
        private Double rating;

        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }
}