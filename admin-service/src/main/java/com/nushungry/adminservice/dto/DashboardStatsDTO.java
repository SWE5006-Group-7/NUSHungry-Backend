package com.nushungry.adminservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DashboardStatsDTO {
    private StatsCards statsCards;
    private SystemOverview systemOverview;
    private List<UserGrowthData> userGrowthData;
    private List<LatestUser> latestUsers;
    private List<LatestReview> latestReviews;

    @Data
    @Builder
    public static class StatsCards {
        private int totalUsers;
        private double userTrend;
        private int totalCafeterias;
        private double cafeteriaTrend;
        private int totalStalls;
        private double stallTrend;
        private int totalReviews;
        private double reviewTrend;
        private int todayOrders;
        private double orderTrend;
    }

    @Data
    @Builder
    public static class SystemOverview {
        private long runningDays;
        private int activeUsers;
        private double activeUserPercentage;
        private int pendingComplaints;
        private double pendingComplaintPercentage;
        private int healthScore;
        private String healthStatus;
    }

    @Data
    @Builder
    public static class UserGrowthData {
        private String date;
        private int count;
    }

    @Data
    @Builder
    public static class LatestUser {
        private Long id;
        private String username;
        private String email;
        private String role;
        private String avatarUrl;
        private boolean enabled;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class LatestReview {
        private Long id;
        private String username;
        private String stallName;
        private double rating;
        private LocalDateTime createdAt;
    }
}