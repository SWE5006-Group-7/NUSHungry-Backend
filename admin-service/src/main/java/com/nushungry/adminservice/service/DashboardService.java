package com.nushungry.adminservice.service;

import com.nushungry.adminservice.dto.DashboardStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    // 系统启动时间（假设系统从30天前开始运行）
    private static final LocalDateTime SYSTEM_START_TIME = LocalDateTime.now().minusDays(30);

    public DashboardStatsDTO getDashboardStats() {
        return DashboardStatsDTO.builder()
                .statsCards(getStatsCards())
                .systemOverview(getSystemOverview())
                .userGrowthData(getUserGrowthData())
                .latestUsers(getLatestUsers())
                .latestReviews(getLatestReviews())
                .build();
    }

    private DashboardStatsDTO.StatsCards getStatsCards() {
        // 模拟数据
        return DashboardStatsDTO.StatsCards.builder()
                .totalUsers(1250)
                .userTrend(12.5)
                .totalCafeterias(8)
                .cafeteriaTrend(0.0)
                .totalStalls(42)
                .stallTrend(5.2)
                .totalReviews(320)
                .reviewTrend(8.3)
                .todayOrders(25)
                .orderTrend(15.0)
                .build();
    }

    private DashboardStatsDTO.SystemOverview getSystemOverview() {
        // 计算系统运行天数
        long runningDays = ChronoUnit.DAYS.between(SYSTEM_START_TIME, LocalDateTime.now());

        // 模拟活跃用户数据
        int activeUsers = 850;
        double activePercentage = 68.0;

        // 模拟投诉数据
        int pendingComplaints = 3;
        double pendingPercentage = 12.0;

        // 计算系统健康度
        int healthScore = calculateHealthScore(activePercentage, pendingPercentage);
        String healthStatus = getHealthStatus(healthScore);

        return DashboardStatsDTO.SystemOverview.builder()
                .runningDays(runningDays)
                .activeUsers(activeUsers)
                .activeUserPercentage(activePercentage)
                .pendingComplaints(pendingComplaints)
                .pendingComplaintPercentage(pendingPercentage)
                .healthScore(healthScore)
                .healthStatus(healthStatus)
                .build();
    }

    private List<DashboardStatsDTO.UserGrowthData> getUserGrowthData() {
        // 默认返回最近7天的数据
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return getUserGrowthData(startDate, endDate);
    }

    public List<DashboardStatsDTO.UserGrowthData> getUserGrowthData(LocalDate startDate, LocalDate endDate) {
        List<DashboardStatsDTO.UserGrowthData> growthData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 模拟数据
        String[] dates = {"2025-10-09", "2025-10-10", "2025-10-11", "2025-10-12", "2025-10-13", "2025-10-14", "2025-10-15"};
        int[] counts = {15, 22, 18, 25, 30, 28, 20};

        for (int i = 0; i < dates.length; i++) {
            growthData.add(DashboardStatsDTO.UserGrowthData.builder()
                    .date(dates[i])
                    .count(counts[i])
                    .build());
        }

        return growthData;
    }

    private List<DashboardStatsDTO.LatestUser> getLatestUsers() {
        // 模拟最新用户数据
        List<DashboardStatsDTO.LatestUser> latestUsers = new ArrayList<>();
        
        latestUsers.add(DashboardStatsDTO.LatestUser.builder()
                .id(1001L)
                .username("alice")
                .email("alice@example.com")
                .role("USER")
                .avatarUrl("/uploads/avatars/avatar1.jpg")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());
        
        latestUsers.add(DashboardStatsDTO.LatestUser.builder()
                .id(1002L)
                .username("bob")
                .email("bob@example.com")
                .role("USER")
                .avatarUrl("/uploads/avatars/avatar2.jpg")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusHours(5))
                .build());
        
        latestUsers.add(DashboardStatsDTO.LatestUser.builder()
                .id(1003L)
                .username("charlie")
                .email("charlie@example.com")
                .role("USER")
                .avatarUrl("/uploads/avatars/avatar3.jpg")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusHours(8))
                .build());
        
        latestUsers.add(DashboardStatsDTO.LatestUser.builder()
                .id(1004L)
                .username("diana")
                .email("diana@example.com")
                .role("USER")
                .avatarUrl("/uploads/avatars/avatar4.jpg")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusHours(12))
                .build());
        
        latestUsers.add(DashboardStatsDTO.LatestUser.builder()
                .id(1005L)
                .username("eve")
                .email("eve@example.com")
                .role("USER")
                .avatarUrl("/uploads/avatars/avatar5.jpg")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusHours(24))
                .build());

        return latestUsers;
    }

    private List<DashboardStatsDTO.LatestReview> getLatestReviews() {
        // 模拟最新评价数据
        List<DashboardStatsDTO.LatestReview> latestReviews = new ArrayList<>();
        
        latestReviews.add(DashboardStatsDTO.LatestReview.builder()
                .id(2001L)
                .username("alice")
                .stallName("汉堡王")
                .rating(4.5)
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build());
        
        latestReviews.add(DashboardStatsDTO.LatestReview.builder()
                .id(2002L)
                .username("bob")
                .stallName("麻辣烫")
                .rating(5.0)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());
        
        latestReviews.add(DashboardStatsDTO.LatestReview.builder()
                .id(2003L)
                .username("charlie")
                .stallName("咖啡屋")
                .rating(4.0)
                .createdAt(LocalDateTime.now().minusHours(5))
                .build());
        
        latestReviews.add(DashboardStatsDTO.LatestReview.builder()
                .id(2004L)
                .username("diana")
                .stallName("沙拉吧")
                .rating(3.5)
                .createdAt(LocalDateTime.now().minusHours(8))
                .build());
        
        latestReviews.add(DashboardStatsDTO.LatestReview.builder()
                .id(2005L)
                .username("eve")
                .stallName("炸鸡店")
                .rating(4.8)
                .createdAt(LocalDateTime.now().minusHours(12))
                .build());

        return latestReviews;
    }

    private double calculateTrend(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) * 100.0) / previous;
    }

    private int calculateHealthScore(double activePercentage, double pendingPercentage) {
        // 基础分数
        int score = 100;

        // 根据活跃用户百分比调整
        if (activePercentage < 30) {
            score -= 20;
        } else if (activePercentage < 50) {
            score -= 10;
        }

        // 根据待处理投诉百分比调整
        if (pendingPercentage > 50) {
            score -= 30;
        } else if (pendingPercentage > 30) {
            score -= 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String getHealthStatus(int healthScore) {
        if (healthScore >= 90) {
            return "优秀";
        } else if (healthScore >= 70) {
            return "良好";
        } else if (healthScore >= 50) {
            return "一般";
        } else {
            return "需要关注";
        }
    }
}